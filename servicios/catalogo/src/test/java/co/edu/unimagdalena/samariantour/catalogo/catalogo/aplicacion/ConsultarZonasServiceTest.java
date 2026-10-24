package co.edu.unimagdalena.samariantour.catalogo.catalogo.aplicacion;

import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.excepcion.ZonaNoEncontradaException;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.Zona;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static co.edu.unimagdalena.samariantour.catalogo.catalogo.aplicacion.EscenarioAplicacion.ZONA_TAGANGA;
import static co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.DatosDePrueba.ZONA_ACTIVA;
import static co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.DatosDePrueba.ZONA_INACTIVA;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ConsultarZonasServiceTest {

    private final EscenarioAplicacion escenario = new EscenarioAplicacion();

    @Test
    @DisplayName("lista solo las zonas activas, ordenadas por nombre")
    void zonasActivas() {
        assertThat(escenario.consultaZonas.consultarActivas()).extracting(Zona::id)
                .containsExactly(ZONA_ACTIVA, ZONA_TAGANGA);
    }

    @Test
    @DisplayName("la ficha de una zona responde aunque esté inactiva, porque sus ofertas siguen visibles (DC-49)")
    void ficha() {
        assertThat(escenario.consultaZonas.consultar(ZONA_ACTIVA).buenasPracticas()).containsExactly("No dejar residuos");
        assertThat(escenario.consultaZonas.consultar(ZONA_INACTIVA).activa()).isFalse();
        assertThatThrownBy(() -> escenario.consultaZonas.consultar("no-existe"))
                .isInstanceOf(ZonaNoEncontradaException.class);
    }
}
