package co.edu.unimagdalena.samariantour.catalogo.catalogo.infraestructura.salida;

import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.excepcion.ZonaDuplicadaException;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.Coordenada;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.DatosZona;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.Zona;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.puerto.salida.ZonaRepositoryPort;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.infraestructura.salida.demo.DatosSantaMarta;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Comparator;
import java.util.List;

import static co.edu.unimagdalena.samariantour.catalogo.catalogo.infraestructura.salida.demo.DatosSantaMarta.TAGANGA;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Contrato de ZonaRepositoryPort, común a los adaptadores en memoria y de MongoDB (DC-49). Cada prueba recibe un
 * repositorio que solo tiene las zonas de DatosSantaMarta.
 */
public interface ContratoZonaRepository {

    Coordenada MINCA = new Coordenada(11.14, -74.12);

    ZonaRepositoryPort zonas();

    @Test
    @DisplayName("devuelve cada zona como se guardó; todas y las activas salen ordenadas por nombre")
    default void idaYVuelta() {
        for (Zona original : DatosSantaMarta.zonas()) {
            assertThat(zonas().buscarPorId(original.id())).contains(original);
        }
        List<Zona> porNombre = DatosSantaMarta.zonas().stream().sorted(Comparator.comparing(Zona::nombre)).toList();
        assertThat(zonas().buscarTodas()).containsExactlyElementsOf(porNombre);
        assertThat(zonas().buscarActivas()).containsExactlyElementsOf(porNombre);
    }

    @Test
    @DisplayName("guarda una zona nueva con un id nuevo, y editarla reemplaza todos sus datos")
    default void guardarYEditar() {
        String id = zonas().nuevoId();
        assertThat(id).isNotEqualTo(zonas().nuevoId()).matches("[0-9a-f]{24}");
        Zona nueva = Zona.registrar(id, datos("Isla de Salamanca"));
        zonas().guardar(nueva);
        assertThat(zonas().buscarPorId(id)).contains(nueva);

        Zona inactiva = nueva.editar(new DatosZona("Isla de Salamanca", "Vía Parque", MINCA, true, false,
                List.of("No alimentar a los animales"), null), false);
        zonas().guardar(inactiva);

        assertThat(zonas().buscarPorId(id)).contains(inactiva);
        assertThat(zonas().buscarTodas()).contains(inactiva);
        assertThat(zonas().buscarActivas()).doesNotContain(inactiva).extracting(Zona::id).doesNotContain(id);
    }

    @Test
    @DisplayName("no guarda dos zonas con el mismo nombre, sin distinguir mayúsculas ni tildes; una zona sí puede "
            + "conservar el suyo")
    default void nombreUnico() {
        assertThatThrownBy(() -> zonas().guardar(Zona.registrar(zonas().nuevoId(), datos("TÁGANGA"))))
                .isInstanceOf(ZonaDuplicadaException.class);

        Zona taganga = zonas().buscarPorId(TAGANGA).orElseThrow();
        Zona conOtraDescripcion = taganga.editar(new DatosZona("Taganga", "Otra descripción", MINCA, false, true,
                List.of(), null), true);
        zonas().guardar(conOtraDescripcion);
        assertThat(zonas().buscarPorId(TAGANGA)).contains(conOtraDescripcion);

        Zona renombrada = conOtraDescripcion.editar(datos("parque  tayrona"), true);
        assertThatThrownBy(() -> zonas().guardar(renombrada)).isInstanceOf(ZonaDuplicadaException.class);
        assertThat(zonas().buscarPorId(TAGANGA)).contains(conOtraDescripcion);
        assertThat(zonas().buscarTodas()).hasSize(DatosSantaMarta.zonas().size());
    }

    private static DatosZona datos(String nombre) {
        return new DatosZona(nombre, "Descripción de la zona", MINCA, false, false, List.of(), null);
    }
}
