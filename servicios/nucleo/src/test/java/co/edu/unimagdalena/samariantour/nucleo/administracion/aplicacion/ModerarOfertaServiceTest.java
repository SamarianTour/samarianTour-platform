package co.edu.unimagdalena.samariantour.nucleo.administracion.aplicacion;

import co.edu.unimagdalena.samariantour.nucleo.administracion.dominio.excepcion.CatalogoNoDisponibleException;
import co.edu.unimagdalena.samariantour.nucleo.administracion.dominio.excepcion.DatoInvalidoException;
import co.edu.unimagdalena.samariantour.nucleo.administracion.dominio.excepcion.ModeracionYaDecididaException;
import co.edu.unimagdalena.samariantour.nucleo.administracion.dominio.excepcion.NoEstaEnModeracionException;
import co.edu.unimagdalena.samariantour.nucleo.administracion.dominio.excepcion.PrestadorNoHabilitadoException;
import co.edu.unimagdalena.samariantour.nucleo.administracion.dominio.excepcion.VersionNoVigenteException;
import co.edu.unimagdalena.samariantour.nucleo.administracion.dominio.modelo.Decision;
import co.edu.unimagdalena.samariantour.nucleo.administracion.dominio.modelo.DecisionModeracion;
import co.edu.unimagdalena.samariantour.nucleo.administracion.dominio.modelo.OfertaEnModeracion;
import co.edu.unimagdalena.samariantour.nucleo.administracion.dominio.puerto.entrada.comando.ModerarOfertaCommand;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static co.edu.unimagdalena.samariantour.nucleo.administracion.aplicacion.EscenarioModeracion.ADMINISTRADOR;
import static co.edu.unimagdalena.samariantour.nucleo.administracion.aplicacion.EscenarioModeracion.AHORA;
import static co.edu.unimagdalena.samariantour.nucleo.administracion.aplicacion.EscenarioModeracion.PRESTADOR;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** El administrador decide sobre una oferta en moderación (RF-79, DN-08). */
class ModerarOfertaServiceTest {

    private static final String LANCHA = "b00000000000000000000017";

    private final EscenarioModeracion escenario = new EscenarioModeracion();

    @BeforeEach
    void ofertaEnModeracion() {
        escenario.ofertas.put(LANCHA, new OfertaEnModeracion(LANCHA, "Paseo en lancha a Playa Blanca", PRESTADOR, 1));
    }

    @Test
    @DisplayName("aprobar guarda la decisión y la envía al catálogo, sin notificar a nadie")
    void aprobar() {
        DecisionModeracion decision = moderar(1, Decision.APROBAR, null);

        assertThat(decision.administradorId()).isEqualTo(ADMINISTRADOR);
        assertThat(decision.decididaEn()).isEqualTo(AHORA);
        assertThat(escenario.historial).containsExactly(decision);
        assertThat(escenario.avisos).containsExactly(decision);
        assertThat(escenario.notificaciones).isEmpty();
    }

    @Test
    @DisplayName("rechazar exige motivo y lo notifica al prestador con el nombre de la oferta")
    void rechazar() {
        assertThatThrownBy(() -> moderar(1, Decision.RECHAZAR, null)).isInstanceOf(DatoInvalidoException.class);

        moderar(1, Decision.RECHAZAR, "Falta el punto de encuentro");

        assertThat(escenario.notificaciones).containsExactly(
                PRESTADOR + " Paseo en lancha a Playa Blanca: Falta el punto de encuentro");
    }

    @Test
    @DisplayName("repetir la misma decisión devuelve la de entonces sin otro aviso; la contraria responde ya decidida")
    void repetida() {
        DecisionModeracion primera = moderar(1, Decision.APROBAR, null);
        // El catálogo ya la aplicó: la oferta dejó de estar en moderación.
        escenario.ofertas.clear();

        assertThat(moderar(1, Decision.APROBAR, null)).isEqualTo(primera);
        assertThatThrownBy(() -> moderar(1, Decision.RECHAZAR, "Otro motivo"))
                .isInstanceOf(ModeracionYaDecididaException.class);
        assertThat(escenario.avisos).hasSize(1);
    }

    @Test
    @DisplayName("si el catálogo no la tiene en moderación, o tiene otra versión, no se decide")
    void sinModeracionOVersionVieja() {
        assertThatThrownBy(() -> escenario.moderarOferta.moderar(new ModerarOfertaCommand("no-existe", 1,
                Decision.APROBAR, null, ADMINISTRADOR))).isInstanceOf(NoEstaEnModeracionException.class);

        escenario.ofertas.put(LANCHA, new OfertaEnModeracion(LANCHA, "Paseo en lancha", PRESTADOR, 2));
        assertThatThrownBy(() -> moderar(1, Decision.APROBAR, null)).isInstanceOf(VersionNoVigenteException.class);

        assertThat(escenario.historial).isEmpty();
        assertThat(escenario.avisos).isEmpty();
    }

    @Test
    @DisplayName("con el catálogo caído no se decide sin verificar")
    void catalogoCaido() {
        escenario.catalogoCaido = true;

        assertThatThrownBy(() -> moderar(1, Decision.APROBAR, null))
                .isInstanceOf(CatalogoNoDisponibleException.class);
        assertThat(escenario.historial).isEmpty();
    }

    private DecisionModeracion moderar(int version, Decision decision, String motivo) {
        return escenario.moderarOferta.moderar(new ModerarOfertaCommand(LANCHA, version, decision, motivo,
                ADMINISTRADOR));
    }

    @Test
    @DisplayName("la oferta de un prestador sin RNT activo no se aprueba, pero sí se puede rechazar (RF-79, DN-35)")
    void prestadorNoHabilitado() {
        escenario.inhabilitados.add(PRESTADOR);

        assertThatThrownBy(() -> moderar(1, Decision.APROBAR, null))
                .isInstanceOf(PrestadorNoHabilitadoException.class);
        assertThat(escenario.avisos).isEmpty();
        assertThat(moderar(1, Decision.RECHAZAR, "El RNT no está activo").decision()).isEqualTo(Decision.RECHAZAR);
    }
}
