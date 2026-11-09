package co.edu.unimagdalena.samariantour.nucleo.pagos.dominio.modelo;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Los fallos al procesar un evento de la pasarela (DN-21). */
class EventoPasarelaTest {

    private static final Instant AHORA = Instant.parse("2026-10-27T15:00:00Z");

    private static EventoPasarela recibido() {
        return EventoPasarela.recibido(UUID.randomUUID(),
                new EventoVerificado("T-1", "ST-1", EstadoTransaccion.APROBADA, 30_000_000, "{}"), AHORA);
    }

    @Test
    @DisplayName("recién recibido está pendiente, sin fallos, y se puede tomar de inmediato")
    void recibidoPendiente() {
        EventoPasarela evento = recibido();

        assertThat(evento.procesamiento()).isEqualTo(EstadoProcesamiento.PENDIENTE);
        assertThat(evento.intentosProcesamiento()).isZero();
        assertThat(evento.siguienteIntentoEn()).isEqualTo(AHORA);
        assertThat(evento.procesado("APLICADO", AHORA).procesamiento()).isEqualTo(EstadoProcesamiento.PROCESADO);
    }

    @Test
    @DisplayName("cada fallo lo aplaza con espera creciente; al quinto queda fallido y sale de la cola")
    void fallos() {
        EventoPasarela evento = recibido();
        Instant ahora = AHORA;
        for (int fallo = 1; fallo <= 4; fallo++) {
            evento = evento.conFallo("IllegalStateException: falló", ahora);
            assertThat(evento.procesamiento()).isEqualTo(EstadoProcesamiento.PENDIENTE);
            assertThat(evento.intentosProcesamiento()).isEqualTo(fallo);
            assertThat(evento.siguienteIntentoEn())
                    .isEqualTo(ahora.plus(Duration.ofSeconds(5).multipliedBy(1L << (fallo - 1))));
            ahora = evento.siguienteIntentoEn();
        }

        evento = evento.conFallo("IllegalStateException: falló otra vez", ahora);

        assertThat(evento.procesamiento()).isEqualTo(EstadoProcesamiento.FALLIDO);
        assertThat(evento.intentosProcesamiento()).isEqualTo(5);
        assertThat(evento.ultimoError()).isEqualTo("IllegalStateException: falló otra vez");
        assertThat(evento.procesadoEn()).isNull();
    }

    @Test
    @DisplayName("el error se recorta a 500 caracteres para que quepa en su columna")
    void errorRecortado() {
        EventoPasarela evento = recibido().conFallo("x".repeat(800), AHORA);

        assertThat(evento.ultimoError()).hasSize(EventoPasarela.LARGO_ERROR);
    }

    @Test
    @DisplayName("fallido, el administrador lo devuelve a la cola sin fallos ni aviso; uno pendiente no (DN-44)")
    void reintentado() {
        EventoPasarela evento = recibido();
        for (int i = 0; i < 5; i++) {
            evento = evento.conFallo("IllegalStateException: falló", AHORA);
        }
        EventoPasarela avisado = evento.avisado(AHORA);
        Instant despues = AHORA.plusSeconds(3600);

        EventoPasarela reintentado = avisado.reintentado(despues);

        assertThat(reintentado.procesamiento()).isEqualTo(EstadoProcesamiento.PENDIENTE);
        assertThat(reintentado.intentosProcesamiento()).isZero();
        assertThat(reintentado.siguienteIntentoEn()).isEqualTo(despues);
        assertThat(reintentado.avisoEnviadoEn()).isNull();
        assertThat(reintentado.ultimoError()).isEqualTo("IllegalStateException: falló");
        assertThatThrownBy(() -> recibido().reintentado(despues))
                .isInstanceOf(IllegalStateException.class);
    }
}
