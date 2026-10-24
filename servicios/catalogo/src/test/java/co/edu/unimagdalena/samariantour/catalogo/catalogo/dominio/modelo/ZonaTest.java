package co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo;

import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.excepcion.DatoInvalidoException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Zonas que registra y edita el administrador (HU-ADM-06, DC-49). */
class ZonaTest {

    private static final Coordenada MINCA = new Coordenada(11.14, -74.12);

    @Test
    @DisplayName("nombre, descripción y ubicación son obligatorios; buenas prácticas y condiciones de visita, no")
    void obligatorios() {
        assertThatThrownBy(() -> datos(" ", "Sierra", MINCA)).isInstanceOf(DatoInvalidoException.class)
                .hasMessageStartingWith("nombre:");
        assertThatThrownBy(() -> datos("Minca", null, MINCA)).isInstanceOf(DatoInvalidoException.class)
                .hasMessageStartingWith("descripcion:");
        assertThatThrownBy(() -> datos("Minca", "Sierra", null)).isInstanceOf(DatoInvalidoException.class)
                .hasMessageStartingWith("ubicacion:");

        DatosZona sinOpcionales = new DatosZona("Minca", "Sierra", MINCA, false, false, null, " ");
        assertThat(sinOpcionales.buenasPracticas()).isEmpty();
        assertThat(sinOpcionales.condicionesVisita()).isNull();
    }

    @Test
    @DisplayName("las buenas prácticas se guardan sin espacios en los extremos y no admiten una en blanco")
    void buenasPracticas() {
        DatosZona datos = new DatosZona("Minca", "Sierra", MINCA, true, false,
                List.of("  No dejar residuos ", "No extraer fauna"), null);
        assertThat(datos.buenasPracticas()).containsExactly("No dejar residuos", "No extraer fauna");

        assertThatThrownBy(() -> new DatosZona("Minca", "Sierra", MINCA, true, false, List.of("No dejar residuos", " "),
                null)).isInstanceOf(DatoInvalidoException.class).hasMessageStartingWith("buenasPracticas:");
        assertThatThrownBy(() -> new DatosZona("Minca", "Sierra", MINCA, true, false,
                Arrays.asList("No dejar residuos", null), null)).isInstanceOf(DatoInvalidoException.class);
    }

    @Test
    @DisplayName("una zona registrada nace activa; editarla reemplaza sus datos, conserva el id y puede inactivarla")
    void registrarYEditar() {
        Zona zona = Zona.registrar("z1", new DatosZona("Minca", "Sierra", MINCA, false, false, List.of(), null));
        assertThat(zona.activa()).isTrue();

        Zona editada = zona.editar(new DatosZona("Minca y Sierra", "Bonda y Minca", MINCA, true, true,
                List.of("No dejar residuos"), "Ingreso hasta las 16:00"), false);

        assertThat(editada).isEqualTo(new Zona("z1", "Minca y Sierra", "Bonda y Minca", MINCA, true, true, false,
                List.of("No dejar residuos"), "Ingreso hasta las 16:00"));
        assertThat(editada.editar(datos("Minca y Sierra", "Bonda y Minca", MINCA), true).activa()).isTrue();
    }

    @Test
    @DisplayName("la clave del nombre ignora mayúsculas, tildes y espacios repetidos")
    void claveNombre() {
        Zona minca = Zona.registrar("z1", datos("Minca", "Sierra", MINCA));
        assertThat(Zona.registrar("z2", datos("  MINCÁ ", "Sierra", MINCA)).claveNombre())
                .isEqualTo(minca.claveNombre());
        assertThat(Zona.registrar("z3", datos("Parque   Tayrona", "Parque", MINCA)).claveNombre())
                .isEqualTo("parque tayrona");
        assertThat(Zona.registrar("z4", datos("Minca alta", "Sierra", MINCA)).claveNombre())
                .isNotEqualTo(minca.claveNombre());
    }

    private static DatosZona datos(String nombre, String descripcion, Coordenada ubicacion) {
        return new DatosZona(nombre, descripcion, ubicacion, false, false, List.of(), null);
    }
}
