package co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.politica;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class PoliticaVentanasTest {

    @ParameterizedTest(name = "atendida a las {0} vence a las {1}")
    @CsvSource({
            // De día: 3 h después.
            "2026-10-24T05:00, 2026-10-24T08:00",
            "2026-10-24T12:15, 2026-10-24T15:15",
            "2026-10-24T18:59, 2026-10-24T21:59",
            // De 19:00 a 23:59: a las 08:00 del día siguiente.
            "2026-10-24T19:00, 2026-10-25T08:00",
            "2026-10-24T23:59, 2026-10-25T08:00",
            // De 00:00 a 04:59: a las 08:00 del mismo día.
            "2026-10-25T00:00, 2026-10-25T08:00",
            "2026-10-25T04:59, 2026-10-25T08:00",
            // El último día del mes también pasa al siguiente.
            "2026-10-31T21:30, 2026-11-01T08:00"
    })
    @DisplayName("ventana de confirmación y pago en la hora de Colombia (RF-55)")
    void ventanaConfirmacion(LocalDateTime atendida, LocalDateTime vence) {
        assertThat(PoliticaVentanas.ventanaConfirmacion(enBogota(atendida))).isEqualTo(enBogota(vence));
    }

    @Test
    @DisplayName("la ventana no depende de la zona del servidor: 23:59 UTC son las 18:59 en Colombia, 3 h")
    void zonaDeColombia() {
        assertThat(PoliticaVentanas.ventanaConfirmacion(Instant.parse("2026-10-24T23:59:00Z")))
                .isEqualTo(Instant.parse("2026-10-25T02:59:00Z"));
    }

    @Test
    @DisplayName("el operador tiene 24 h desde la creación (RF-41)")
    void plazoOperador() {
        assertThat(PoliticaVentanas.plazoOperador(Instant.parse("2026-10-24T15:00:00Z")))
                .isEqualTo(Instant.parse("2026-10-25T15:00:00Z"));
    }

    @Test
    @DisplayName("con un intento de pago en curso, el vencimiento espera hasta 30 min (RF-55)")
    void gracia() {
        assertThat(PoliticaVentanas.conGracia(Instant.parse("2026-10-24T18:00:00Z")))
                .isEqualTo(Instant.parse("2026-10-24T18:30:00Z"));
    }

    @Test
    @DisplayName("próxima a vencer: le quedan 6 h o menos, incluso si ya pasó (RF-36)")
    void proximaAVencer() {
        Instant vence = Instant.parse("2026-10-25T15:00:00Z");

        assertThat(PoliticaVentanas.proximaAVencer(vence, vence.minus(Duration.ofHours(6)).minusSeconds(1)))
                .isFalse();
        assertThat(PoliticaVentanas.proximaAVencer(vence, vence.minus(Duration.ofHours(6)))).isTrue();
        assertThat(PoliticaVentanas.proximaAVencer(vence, vence.minusSeconds(1))).isTrue();
        assertThat(PoliticaVentanas.proximaAVencer(vence, vence.plusSeconds(30))).isTrue();
    }

    private static Instant enBogota(LocalDateTime hora) {
        return hora.atZone(PoliticaVentanas.ZONA).toInstant();
    }
}
