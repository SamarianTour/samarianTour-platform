package co.edu.unimagdalena.samariantour.nucleo.pagos.dominio.modelo;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class IntentoPagoTest {

    private static final Instant AHORA = Instant.parse("2026-10-27T15:00:00Z");
    private static final UUID SOLICITUD = UUID.randomUUID();
    private static final UUID TURISTA = UUID.randomUUID();

    private static IntentoPago nuevo() {
        return IntentoPago.nuevo(UUID.randomUUID(), SOLICITUD, TURISTA, 1, 300_000, AHORA);
    }

    @Test
    @DisplayName("nace en curso por el precio base exacto en centavos, con una referencia única (RF-46, RF-49)")
    void nuevoIntento() {
        IntentoPago intento = nuevo();

        assertThat(intento.estado()).isEqualTo(EstadoIntento.EN_CURSO);
        assertThat(intento.montoCentavos()).isEqualTo(30_000_000);
        assertThat(intento.referencia()).isEqualTo("ST-" + intento.id());
        assertThat(intento.transaccionId()).isNull();
        assertThatThrownBy(() -> IntentoPago.nuevo(UUID.randomUUID(), SOLICITUD, TURISTA, 1, 0, AHORA))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("aprobado: guarda el id de la transacción (RF-52); el mismo evento otra vez no cambia nada")
    void aprobado() {
        IntentoPago intento = nuevo();

        assertThat(intento.aplicar(EstadoTransaccion.APROBADA, "1234-1610641025-49201", AHORA.plusSeconds(60)))
                .isEqualTo(ResultadoIntento.APROBADO);
        assertThat(intento.estado()).isEqualTo(EstadoIntento.APROBADO);
        assertThat(intento.transaccionId()).isEqualTo("1234-1610641025-49201");
        assertThat(intento.resueltoEn()).isEqualTo(AHORA.plusSeconds(60));
        assertThat(intento.aplicar(EstadoTransaccion.APROBADA, "1234-1610641025-49201", AHORA.plusSeconds(90)))
                .isEqualTo(ResultadoIntento.SIN_CAMBIOS);
        assertThat(intento.aplicar(EstadoTransaccion.ANULADA, "1234-1610641025-49201", AHORA.plusSeconds(90)))
                .isEqualTo(ResultadoIntento.SIN_CAMBIOS);
        assertThat(intento.estado()).isEqualTo(EstadoIntento.APROBADO);
    }

    @Test
    @DisplayName("otra transacción aprobada con un intento ya resuelto: no lo cambia, pero se informa (DN-22)")
    void aprobadoSinEfecto() {
        IntentoPago rechazado = nuevo();
        IntentoPago aprobado = nuevo();
        rechazado.aplicar(EstadoTransaccion.RECHAZADA, "t-1", AHORA);
        aprobado.aplicar(EstadoTransaccion.APROBADA, "t-1", AHORA);

        assertThat(rechazado.aplicar(EstadoTransaccion.APROBADA, "t-2", AHORA.plusSeconds(60)))
                .isEqualTo(ResultadoIntento.APROBADO_SIN_EFECTO);
        assertThat(aprobado.aplicar(EstadoTransaccion.APROBADA, "t-2", AHORA.plusSeconds(60)))
                .isEqualTo(ResultadoIntento.APROBADO_SIN_EFECTO);
        assertThat(rechazado.estado()).isEqualTo(EstadoIntento.RECHAZADO);
        assertThat(rechazado.transaccionId()).isEqualTo("t-1");
        assertThat(aprobado.transaccionId()).isEqualTo("t-1");
    }

    @Test
    @DisplayName("pendiente: solo guarda el id de la transacción y sigue en curso; la que lo resuelve queda guardada")
    void pendiente() {
        IntentoPago intento = nuevo();

        assertThat(intento.aplicar(EstadoTransaccion.PENDIENTE, "t-1", AHORA)).isEqualTo(ResultadoIntento.SIN_CAMBIOS);

        assertThat(intento.estado()).isEqualTo(EstadoIntento.EN_CURSO);
        assertThat(intento.transaccionId()).isEqualTo("t-1");
        assertThat(intento.tieneCambios()).isTrue();
        assertThat(intento.aplicar(EstadoTransaccion.APROBADA, "t-2", AHORA)).isEqualTo(ResultadoIntento.APROBADO);
        assertThat(intento.transaccionId()).isEqualTo("t-2");
    }

    @Test
    @DisplayName("rechazado, anulado o con error: los tres llevan la solicitud a Pago fallido (RF-58)")
    void rechazos() {
        IntentoPago rechazado = nuevo();
        IntentoPago anulado = nuevo();
        IntentoPago conError = nuevo();

        assertThat(rechazado.aplicar(EstadoTransaccion.RECHAZADA, "t-1", AHORA)).isEqualTo(ResultadoIntento.RECHAZADO);
        assertThat(anulado.aplicar(EstadoTransaccion.ANULADA, "t-2", AHORA)).isEqualTo(ResultadoIntento.RECHAZADO);
        assertThat(conError.aplicar(EstadoTransaccion.ERROR, "t-3", AHORA)).isEqualTo(ResultadoIntento.RECHAZADO);

        assertThat(rechazado.estado()).isEqualTo(EstadoIntento.RECHAZADO);
        assertThat(anulado.estado()).isEqualTo(EstadoIntento.RECHAZADO);
        assertThat(conError.estado()).isEqualTo(EstadoIntento.ERROR);
        assertThat(conError.estado().tieneResultado()).isTrue();
    }

    @Test
    @DisplayName("sin resultado a los 30 min queda abandonado; no cuenta para el límite, pero su resultado tardío vale")
    void abandonado() {
        IntentoPago intento = nuevo();

        assertThat(intento.abandonarSiCorresponde(AHORA.plus(Duration.ofMinutes(29)))).isFalse();
        assertThat(intento.abandonarSiCorresponde(AHORA.plus(Duration.ofMinutes(30)))).isTrue();
        assertThat(intento.estado()).isEqualTo(EstadoIntento.ABANDONADO);
        assertThat(intento.estado().tieneResultado()).isFalse();
        assertThat(intento.abandonarSiCorresponde(AHORA.plus(Duration.ofMinutes(31)))).isFalse();

        assertThat(intento.aplicar(EstadoTransaccion.APROBADA, "t-1", AHORA.plus(Duration.ofMinutes(40))))
                .isEqualTo(ResultadoIntento.APROBADO);
        assertThat(intento.estado()).isEqualTo(EstadoIntento.APROBADO);
    }
}
