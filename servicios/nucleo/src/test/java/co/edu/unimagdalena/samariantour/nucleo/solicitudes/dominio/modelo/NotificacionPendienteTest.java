package co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.modelo;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class NotificacionPendienteTest {

    private static final Instant AHORA = Instant.parse("2026-10-24T15:00:00Z");

    @Test
    @DisplayName("enviada: cuenta el intento y guarda la hora")
    void enviada() {
        NotificacionPendiente notificacion = nueva();

        notificacion.registrarEnvio(AHORA);

        assertThat(notificacion.estado()).isEqualTo(EstadoEnvio.ENVIADA);
        assertThat(notificacion.intentos()).isEqualTo(1);
        assertThat(notificacion.enviadaEn()).isEqualTo(AHORA);
    }

    @Test
    @DisplayName("si falla se reprograma tras 1, 2, 4 y 8 minutos; al quinto intento queda fallida")
    void esperaCreciente() {
        NotificacionPendiente notificacion = nueva();

        for (long minutos : new long[]{1, 2, 4, 8}) {
            notificacion.registrarFallo("sin conexión", AHORA);
            assertThat(notificacion.estado()).isEqualTo(EstadoEnvio.PENDIENTE);
            assertThat(notificacion.proximoIntento()).isEqualTo(AHORA.plus(Duration.ofMinutes(minutos)));
        }
        notificacion.registrarFallo("sin conexión", AHORA);

        assertThat(notificacion.estado()).isEqualTo(EstadoEnvio.FALLIDA);
        assertThat(notificacion.intentos()).isEqualTo(NotificacionPendiente.INTENTOS_MAXIMOS);
        assertThat(notificacion.ultimoError()).isEqualTo("sin conexión");
    }

    @Test
    @DisplayName("el error se recorta a 500 caracteres")
    void errorRecortado() {
        NotificacionPendiente notificacion = nueva();

        notificacion.registrarFallo("x".repeat(800), AHORA);

        assertThat(notificacion.ultimoError()).hasSize(NotificacionPendiente.LONGITUD_MAXIMA_ERROR);
    }

    private static NotificacionPendiente nueva() {
        return new NotificacionPendiente(UUID.randomUUID(), UUID.randomUUID(),
                Destinatario.turista(UUID.randomUUID()), TipoNotificacion.SOLICITUD_RECIBIDA, Map.of(), 0);
    }
}
