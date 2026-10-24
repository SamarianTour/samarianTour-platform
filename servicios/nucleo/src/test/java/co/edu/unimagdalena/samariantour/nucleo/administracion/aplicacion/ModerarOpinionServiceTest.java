package co.edu.unimagdalena.samariantour.nucleo.administracion.aplicacion;

import co.edu.unimagdalena.samariantour.nucleo.administracion.dominio.excepcion.ModeracionYaDecididaException;
import co.edu.unimagdalena.samariantour.nucleo.administracion.dominio.excepcion.NoEstaEnModeracionException;
import co.edu.unimagdalena.samariantour.nucleo.administracion.dominio.modelo.Decision;
import co.edu.unimagdalena.samariantour.nucleo.administracion.dominio.modelo.DecisionModeracion;
import co.edu.unimagdalena.samariantour.nucleo.administracion.dominio.modelo.OpinionEnModeracion;
import co.edu.unimagdalena.samariantour.nucleo.administracion.dominio.modelo.TipoModerado;
import co.edu.unimagdalena.samariantour.nucleo.administracion.dominio.puerto.entrada.comando.ModerarOpinionCommand;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static co.edu.unimagdalena.samariantour.nucleo.administracion.aplicacion.EscenarioModeracion.ADMINISTRADOR;
import static co.edu.unimagdalena.samariantour.nucleo.administracion.aplicacion.EscenarioModeracion.TURISTA;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** El administrador decide sobre una opinión en moderación (RF-80, DN-08). */
class ModerarOpinionServiceTest {

    private static final String OPINION = "d0000000000000000000000b";

    private final EscenarioModeracion escenario = new EscenarioModeracion();

    @Test
    @DisplayName("rechazar una opinión la envía al catálogo sin versión y notifica al turista con el motivo")
    void rechazar() {
        escenario.opiniones.put(OPINION, new OpinionEnModeracion(OPINION, "b00000000000000000000006", TURISTA));

        DecisionModeracion decision = moderar(Decision.RECHAZAR, "Lenguaje ofensivo");

        assertThat(decision.tipo()).isEqualTo(TipoModerado.OPINION);
        assertThat(decision.numeroVersion()).isNull();
        assertThat(escenario.avisos).containsExactly(decision);
        assertThat(escenario.notificaciones).containsExactly(TURISTA + ": Lenguaje ofensivo");
    }

    @Test
    @DisplayName("repetida igual devuelve la de entonces; la contraria responde ya decidida; sin moderación, 404")
    void repetidaOAusente() {
        assertThatThrownBy(() -> moderar(Decision.APROBAR, null)).isInstanceOf(NoEstaEnModeracionException.class);

        escenario.opiniones.put(OPINION, new OpinionEnModeracion(OPINION, "b00000000000000000000006", TURISTA));
        DecisionModeracion primera = moderar(Decision.APROBAR, null);
        escenario.opiniones.clear();

        assertThat(moderar(Decision.APROBAR, null)).isEqualTo(primera);
        assertThatThrownBy(() -> moderar(Decision.RECHAZAR, "Tarde"))
                .isInstanceOf(ModeracionYaDecididaException.class);
    }

    private DecisionModeracion moderar(Decision decision, String motivo) {
        return escenario.moderarOpinion.moderar(new ModerarOpinionCommand(OPINION, decision, motivo, ADMINISTRADOR));
    }
}
