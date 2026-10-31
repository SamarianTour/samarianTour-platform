package co.edu.unimagdalena.samariantour.nucleo.pagos.dominio.politica;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PoliticaReintentosTest {

    @Test
    @DisplayName("tres intentos de pago por solicitud: el primero y dos reintentos (RF-58)")
    void intentosDePago() {
        assertThat(PoliticaReintentos.admiteOtroIntento(0)).isTrue();
        assertThat(PoliticaReintentos.admiteOtroIntento(2)).isTrue();
        assertThat(PoliticaReintentos.admiteOtroIntento(3)).isFalse();
    }

    @Test
    @DisplayName("la espera de la dispersión se duplica: con 1 min de base, 1, 2 y 4 min (RF-54)")
    void esperaCreciente() {
        Duration base = Duration.ofMinutes(1);

        assertThat(PoliticaReintentos.esperaDispersion(1, base)).isEqualTo(Duration.ofMinutes(1));
        assertThat(PoliticaReintentos.esperaDispersion(2, base)).isEqualTo(Duration.ofMinutes(2));
        assertThat(PoliticaReintentos.esperaDispersion(3, base)).isEqualTo(Duration.ofMinutes(4));
        assertThatThrownBy(() -> PoliticaReintentos.esperaDispersion(0, base))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("un intento sin resultado se abandona a los 30 min, el máximo que aplaza el vencimiento (RF-55)")
    void abandono() {
        assertThat(PoliticaReintentos.ABANDONO_INTENTO).isEqualTo(Duration.ofMinutes(30));
        assertThat(PoliticaReintentos.REINTENTOS_DISPERSION).isEqualTo(3);
    }

    @Test
    @DisplayName("abandono: a los 30 min de abierto, o al terminar la gracia si la ventana vence antes (RF-55)")
    void horaDeAbandono() {
        Instant trece = Instant.parse("2026-10-27T18:00:00Z");

        assertThat(PoliticaReintentos.abandonoDe(trece.minus(Duration.ofMinutes(20)), trece))
                .isEqualTo(trece.plus(Duration.ofMinutes(30)));
        assertThat(PoliticaReintentos.abandonoDe(trece.minus(Duration.ofMinutes(30)), trece))
                .isEqualTo(trece);
        assertThat(PoliticaReintentos.abandonoDe(trece.minus(Duration.ofHours(2)), trece))
                .isEqualTo(trece.minus(Duration.ofMinutes(90)));
        assertThat(PoliticaReintentos.abandonoDe(trece, null)).isEqualTo(trece.plus(Duration.ofMinutes(30)));
    }

    @Test
    @DisplayName("un evento que falla se reintenta a los 5, 10, 20 y 40 s, hasta 5 min, y queda fallido al quinto")
    void esperaDeEventos() {
        assertThat(PoliticaReintentos.esperaEvento(1)).isEqualTo(Duration.ofSeconds(5));
        assertThat(PoliticaReintentos.esperaEvento(2)).isEqualTo(Duration.ofSeconds(10));
        assertThat(PoliticaReintentos.esperaEvento(4)).isEqualTo(Duration.ofSeconds(40));
        assertThat(PoliticaReintentos.esperaEvento(30)).isEqualTo(Duration.ofMinutes(5));
        assertThat(PoliticaReintentos.eventoFallido(4)).isFalse();
        assertThat(PoliticaReintentos.eventoFallido(5)).isTrue();
        assertThatThrownBy(() -> PoliticaReintentos.esperaEvento(0)).isInstanceOf(IllegalArgumentException.class);
    }
}
