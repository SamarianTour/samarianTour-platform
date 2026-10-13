package co.edu.unimagdalena.samariantour.catalogo.catalogo.aplicacion;

import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.Zona;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static co.edu.unimagdalena.samariantour.catalogo.catalogo.aplicacion.EscenarioAplicacion.ZONA_TAGANGA;
import static co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.DatosDePrueba.ZONA_ACTIVA;
import static org.assertj.core.api.Assertions.assertThat;

class ConsultarZonasServiceTest {

    private final EscenarioAplicacion escenario = new EscenarioAplicacion();

    @Test
    @DisplayName("lista solo las zonas activas, ordenadas por nombre")
    void zonasActivas() {
        assertThat(escenario.consultaZonas.consultarActivas()).extracting(Zona::id)
                .containsExactly(ZONA_ACTIVA, ZONA_TAGANGA);
    }
}
