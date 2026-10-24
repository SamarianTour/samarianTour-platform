package co.edu.unimagdalena.samariantour.catalogo.catalogo.aplicacion;

import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.excepcion.ZonaDuplicadaException;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.excepcion.ZonaNoDisponibleException;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.excepcion.ZonaNoEncontradaException;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.Coordenada;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.DatosZona;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.Oferta;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.Zona;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static co.edu.unimagdalena.samariantour.catalogo.catalogo.aplicacion.EscenarioAplicacion.AGENCIA;
import static co.edu.unimagdalena.samariantour.catalogo.catalogo.aplicacion.EscenarioAplicacion.actividad;
import static co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.DatosDePrueba.ZONA_ACTIVA;
import static co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.DatosDePrueba.ZONA_INACTIVA;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** El administrador gestiona las zonas (HU-ADM-06, RF-84, DC-49). */
class GestionarZonasServiceTest {

    private static final Coordenada MINCA = new Coordenada(11.14, -74.12);

    private final EscenarioAplicacion escenario = new EscenarioAplicacion();

    @Test
    @DisplayName("registra una zona activa que aparece en la lista pública y en la del administrador")
    void registrar() {
        Zona minca = escenario.gestionZonas.registrar(datos("Minca"));

        assertThat(minca.activa()).isTrue();
        assertThat(escenario.consultaZonas.consultarActivas()).contains(minca);
        assertThat(escenario.gestionZonas.listarTodas()).extracting(Zona::nombre)
                .containsExactly("Minca", "Parque Tayrona", "Taganga", "Zona cerrada");
    }

    @Test
    @DisplayName("no registra dos zonas con el mismo nombre, sin distinguir mayúsculas ni tildes")
    void nombreUnico() {
        escenario.gestionZonas.registrar(datos("Minca"));

        assertThatThrownBy(() -> escenario.gestionZonas.registrar(datos("MINCÁ")))
                .isInstanceOf(ZonaDuplicadaException.class);
        assertThatThrownBy(() -> escenario.gestionZonas.registrar(datos("parque tayrona")))
                .isInstanceOf(ZonaDuplicadaException.class);
    }

    @Test
    @DisplayName("editar reemplaza los datos y las marcas; la zona puede conservar su nombre pero no tomar el de otra")
    void editar() {
        Zona minca = escenario.gestionZonas.registrar(datos("Minca"));

        Zona editada = escenario.gestionZonas.editar(minca.id(), new DatosZona("Minca", "Bonda y Minca", MINCA, true,
                true, List.of("No dejar residuos"), "Ingreso hasta las 16:00"), true);

        assertThat(escenario.consultaZonas.consultar(minca.id())).isEqualTo(editada);
        assertThat(editada.areaProtegida()).isTrue();
        assertThat(editada.buenasPracticas()).containsExactly("No dejar residuos");
        assertThatThrownBy(() -> escenario.gestionZonas.editar(minca.id(), datos("Taganga"), true))
                .isInstanceOf(ZonaDuplicadaException.class);
        assertThatThrownBy(() -> escenario.gestionZonas.editar("no-existe", datos("Otra"), true))
                .isInstanceOf(ZonaNoEncontradaException.class);
    }

    @Test
    @DisplayName("una zona inactiva sale de la lista pública; sus ofertas siguen visibles pero no se publica en ella")
    void inactivar() {
        Oferta caminata = escenario.ofertaPublicada("Caminata", "50000", ZONA_ACTIVA);
        Zona tayrona = escenario.consultaZonas.consultar(ZONA_ACTIVA);

        escenario.gestionZonas.editar(ZONA_ACTIVA, datos(tayrona.nombre()), false);

        assertThat(escenario.consultaZonas.consultarActivas()).extracting(Zona::id).doesNotContain(ZONA_ACTIVA);
        assertThat(escenario.consultaZonas.consultar(ZONA_ACTIVA).activa()).isFalse();
        assertThat(escenario.buscarTodas().contenido()).extracting(Oferta::id).contains(caminata.id());
        assertThat(escenario.ficha.consultar(caminata.id()).zona()).hasValueSatisfying(
                zona -> assertThat(zona.activa()).isFalse());
        assertThatThrownBy(() -> escenario.publicar(AGENCIA, actividad("Kayak", "60000", ZONA_ACTIVA)))
                .isInstanceOf(ZonaNoDisponibleException.class);
    }

    @Test
    @DisplayName("una zona inactiva se puede reactivar y vuelve a admitir ofertas")
    void reactivar() {
        escenario.ofertaPublicada("Caminata", "50000", ZONA_ACTIVA);
        Zona cerrada = escenario.consultaZonas.consultar(ZONA_INACTIVA);

        escenario.gestionZonas.editar(ZONA_INACTIVA, new DatosZona(cerrada.nombre(), "Abierta otra vez", MINCA,
                false, false, List.of(), null), true);

        assertThat(escenario.consultaZonas.consultarActivas()).extracting(Zona::id).contains(ZONA_INACTIVA);
        assertThat(escenario.publicar(AGENCIA, actividad("Kayak", "60000", ZONA_INACTIVA)).id()).isNotBlank();
    }

    private static DatosZona datos(String nombre) {
        return new DatosZona(nombre, "Descripción de la zona", MINCA, false, false, List.of(), null);
    }
}
