package co.edu.unimagdalena.samariantour.nucleo.pagos.dominio.modelo;

import co.edu.unimagdalena.samariantour.nucleo.pagos.dominio.excepcion.DatoInvalidoException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ConfiguracionComisionTest {

    private static final Instant AHORA = Instant.parse("2026-10-27T15:00:00Z");

    @Test
    @DisplayName("de 0 a menos de 100, con hasta dos decimales; el inicial es 4 % (RF-53, RF-82)")
    void porcentaje() {
        assertThat(new ConfiguracionComision(new BigDecimal("5.5"), AHORA, null).porcentaje()).isEqualTo("5.50");
        assertThat(new ConfiguracionComision(BigDecimal.ZERO, AHORA, null).porcentaje()).isEqualTo("0.00");
        assertThat(ConfiguracionComision.PORCENTAJE_INICIAL).isEqualTo("4.00");

        assertThatThrownBy(() -> new ConfiguracionComision(null, AHORA, null))
                .isInstanceOf(DatoInvalidoException.class).hasMessageStartingWith("porcentaje:");
        assertThatThrownBy(() -> new ConfiguracionComision(new BigDecimal("-1"), AHORA, null))
                .isInstanceOf(DatoInvalidoException.class);
        assertThatThrownBy(() -> new ConfiguracionComision(new BigDecimal("100"), AHORA, null))
                .isInstanceOf(DatoInvalidoException.class);
        assertThatThrownBy(() -> new ConfiguracionComision(new BigDecimal("4.125"), AHORA, null))
                .isInstanceOf(DatoInvalidoException.class).hasMessageContaining("dos decimales");
    }
}
