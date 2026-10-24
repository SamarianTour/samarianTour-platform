package co.edu.unimagdalena.samariantour.nucleo.administracion.dominio.modelo;

import co.edu.unimagdalena.samariantour.nucleo.administracion.dominio.excepcion.DatoInvalidoException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** La decisión de moderación (DN-08). */
class DecisionModeracionTest {

    private static final UUID ADMINISTRADOR = UUID.fromString("60000000-0000-0000-0000-000000000006");
    private static final Instant AHORA = Instant.parse("2026-10-24T15:00:00Z");

    @Test
    @DisplayName("el aviso de una oferta se identifica por la oferta y la versión; el de una opinión, por la opinión")
    void idAvisoEstable() {
        assertThat(DecisionModeracion.deOferta("b17", 2, Decision.APROBAR, null, ADMINISTRADOR, AHORA).idAviso())
                .isEqualTo("moderacion-oferta-b17-v2");
        assertThat(DecisionModeracion.deOpinion("d01", Decision.APROBAR, null, ADMINISTRADOR, AHORA).idAviso())
                .isEqualTo("moderacion-opinion-d01");
    }

    @Test
    @DisplayName("rechazar exige motivo, de hasta 1000 caracteres; aprobar no lo exige")
    void motivo() {
        assertThatThrownBy(() -> DecisionModeracion.deOferta("b17", 1, Decision.RECHAZAR, "  ", ADMINISTRADOR, AHORA))
                .isInstanceOf(DatoInvalidoException.class).hasMessageStartingWith("motivo:");
        assertThatThrownBy(() -> DecisionModeracion.deOpinion("d01", Decision.RECHAZAR, "x".repeat(1001),
                ADMINISTRADOR, AHORA)).isInstanceOf(DatoInvalidoException.class);

        assertThat(DecisionModeracion.deOferta("b17", 1, Decision.RECHAZAR, " Sin fotos ", ADMINISTRADOR, AHORA)
                .motivo()).isEqualTo("Sin fotos");
        assertThat(DecisionModeracion.deOferta("b17", 1, Decision.APROBAR, null, ADMINISTRADOR, AHORA).motivo())
                .isNull();
    }

    @Test
    @DisplayName("dos decisiones coinciden si dicen lo mismo, aunque cambie el motivo")
    void coincide() {
        var rechazo = DecisionModeracion.deOferta("b17", 1, Decision.RECHAZAR, "Sin fotos", ADMINISTRADOR, AHORA);

        assertThat(rechazo.coincideCon(DecisionModeracion.deOferta("b17", 1, Decision.RECHAZAR, "Otro motivo",
                ADMINISTRADOR, AHORA))).isTrue();
        assertThat(rechazo.coincideCon(DecisionModeracion.deOferta("b17", 1, Decision.APROBAR, null, ADMINISTRADOR,
                AHORA))).isFalse();
    }

    @Test
    @DisplayName("la fecha se guarda en microsegundos, como la devuelve PostgreSQL al repetir la decisión")
    void fechaEnMicrosegundos() {
        Instant conNanos = Instant.parse("2026-10-24T15:00:00.123456789Z");

        assertThat(DecisionModeracion.deOpinion("d01", Decision.APROBAR, null, ADMINISTRADOR, conNanos).decididaEn())
                .isEqualTo(Instant.parse("2026-10-24T15:00:00.123456Z"));
    }
}
