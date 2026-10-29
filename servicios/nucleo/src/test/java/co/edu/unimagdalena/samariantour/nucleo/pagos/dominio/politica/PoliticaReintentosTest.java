package co.edu.unimagdalena.samariantour.nucleo.pagos.dominio.politica;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;

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
}
