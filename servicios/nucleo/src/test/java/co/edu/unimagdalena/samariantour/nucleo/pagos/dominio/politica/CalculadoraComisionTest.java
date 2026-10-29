package co.edu.unimagdalena.samariantour.nucleo.pagos.dominio.politica;

import co.edu.unimagdalena.samariantour.nucleo.pagos.dominio.modelo.ConfiguracionComision;
import co.edu.unimagdalena.samariantour.nucleo.pagos.dominio.modelo.CostoTransaccional;
import co.edu.unimagdalena.samariantour.nucleo.pagos.dominio.modelo.Liquidacion;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CalculadoraComisionTest {

    /** La tarifa publicada del Plan Avanzado de Wompi: 2,65 % + $700 + IVA del 19 %. */
    private static final CostoTransaccional WOMPI = new CostoTransaccional(new BigDecimal("2.65"), 700,
            new BigDecimal("19"));
    private static final BigDecimal CUATRO = new BigDecimal("4.00");

    @Test
    @DisplayName("300.000 al 4 %: comisión 12.000, costo (7.950 + 700) × 1,19 = 10.294 y neto 277.706 (RF-53, RF-54)")
    void caminata() {
        Liquidacion liquidacion = CalculadoraComision.liquidar(300_000, CUATRO, WOMPI);

        assertThat(liquidacion.montoBruto()).isEqualTo(300_000);
        assertThat(liquidacion.porcentajeComision()).isEqualByComparingTo("4");
        assertThat(liquidacion.comision()).isEqualTo(12_000);
        assertThat(liquidacion.costoTransaccional()).isEqualTo(10_294);
        assertThat(liquidacion.montoNeto()).isEqualTo(277_706);
    }

    @Test
    @DisplayName("redondea cada valor a pesos enteros con la mitad hacia arriba")
    void redondeo() {
        // 2,5 % de 100 = 2,5 → 3; 4 % de 12.345 = 493,8 → 494; 4,5 % de 333 = 14,985 → 15.
        assertThat(CalculadoraComision.liquidar(100, new BigDecimal("2.50"), CostoTransaccional.ninguno())
                .comision()).isEqualTo(3);
        assertThat(CalculadoraComision.liquidar(12_345, CUATRO, CostoTransaccional.ninguno()).comision())
                .isEqualTo(494);
        assertThat(CalculadoraComision.liquidar(333, new BigDecimal("4.50"), CostoTransaccional.ninguno())
                .comision()).isEqualTo(15);
        // 2 % de 1.249 = 24,98 → 25: la comisión y el costo se redondean por separado.
        Liquidacion separados = CalculadoraComision.liquidar(1_249, CUATRO,
                new CostoTransaccional(new BigDecimal("2"), 0, BigDecimal.ZERO));
        assertThat(separados.comision()).isEqualTo(50);
        assertThat(separados.costoTransaccional()).isEqualTo(25);
        assertThat(separados.montoNeto()).isEqualTo(1_174);
    }

    @Test
    @DisplayName("el neto nunca es negativo: un precio menor que el costo fijo deja 0 para dispersar")
    void netoNoNegativo() {
        assertThat(CalculadoraComision.liquidar(500, CUATRO, WOMPI).montoNeto()).isZero();
        assertThatThrownBy(() -> CalculadoraComision.liquidar(0, CUATRO, WOMPI))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("el porcentaje vigente es el último que ya empezó; uno posterior no toca lo anterior (RF-83)")
    void vigente() {
        Instant enero = Instant.parse("2026-01-01T00:00:00Z");
        Instant octubre = Instant.parse("2026-10-27T15:00:00Z");
        List<ConfiguracionComision> historial = List.of(
                new ConfiguracionComision(new BigDecimal("5"), octubre, UUID.randomUUID()),
                new ConfiguracionComision(CUATRO, enero, null));

        assertThat(CalculadoraComision.vigenteEn(octubre.minusSeconds(1), historial).porcentaje())
                .isEqualByComparingTo("4");
        assertThat(CalculadoraComision.vigenteEn(octubre, historial).porcentaje()).isEqualByComparingTo("5");
        assertThatThrownBy(() -> CalculadoraComision.vigenteEn(enero.minusSeconds(1), historial))
                .isInstanceOf(IllegalStateException.class);
    }
}
