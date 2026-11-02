package co.edu.unimagdalena.samariantour.nucleo.indicadores.dominio.politica;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

/** Solo datos agregados (RET-03, DN-41). */
class ReglaAnonimizacionTest {

    @Test
    @DisplayName("1 y 2 no se publican; 0 y desde 3, sí")
    void publicable() {
        assertThat(ReglaAnonimizacion.publicable(0)).isZero();
        assertThat(ReglaAnonimizacion.publicable(1)).isNull();
        assertThat(ReglaAnonimizacion.publicable(2)).isNull();
        assertThat(ReglaAnonimizacion.publicable(3)).isEqualTo(3);
    }

    @Test
    @DisplayName("el porcentaje tiene un decimal y se oculta con su conteo o sin total")
    void porcentaje() {
        assertThat(ReglaAnonimizacion.porcentaje(17, 42)).isEqualByComparingTo(new BigDecimal("40.5"));
        assertThat(ReglaAnonimizacion.porcentaje(0, 42)).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(ReglaAnonimizacion.porcentaje(2, 42)).isNull();
        assertThat(ReglaAnonimizacion.porcentaje(0, 0)).isNull();
    }
}
