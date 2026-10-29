package co.edu.unimagdalena.samariantour.nucleo.pagos.dominio.politica;

import co.edu.unimagdalena.samariantour.nucleo.pagos.dominio.modelo.ConfiguracionComision;
import co.edu.unimagdalena.samariantour.nucleo.pagos.dominio.modelo.CostoTransaccional;
import co.edu.unimagdalena.samariantour.nucleo.pagos.dominio.modelo.Liquidacion;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.Collection;
import java.util.Comparator;

/**
 * Reparte un pago confirmado (RF-53, RF-54, RF-83, DN-23), en pesos enteros:
 * <ul>
 *   <li>comisión: el precio base por el porcentaje vigente al confirmarse el pago;</li>
 *   <li>costo transaccional: el porcentaje de la pasarela más su valor fijo, más el IVA sobre los dos;</li>
 *   <li>neto: el precio base menos la comisión y el costo; nunca negativo [SUPUESTO].</li>
 * </ul>
 * Cada valor se redondea a pesos enteros al final de su cálculo, con la mitad hacia arriba [SUPUESTO]: el peso es la
 * unidad mínima que se transfiere y la comisión se calcula una sola vez por pago.
 */
public final class CalculadoraComision {

    private static final BigDecimal CIEN = new BigDecimal("100");

    private CalculadoraComision() {
    }

    public static Liquidacion liquidar(long precioBase, BigDecimal porcentajeComision, CostoTransaccional costo) {
        if (precioBase <= 0) {
            throw new IllegalArgumentException("El precio base debe ser positivo");
        }
        BigDecimal precio = BigDecimal.valueOf(precioBase);
        long comision = pesos(precio.multiply(porcentajeComision).divide(CIEN));
        BigDecimal tarifa = precio.multiply(costo.porcentaje()).divide(CIEN).add(BigDecimal.valueOf(costo.fijo()));
        long costoTransaccional = pesos(tarifa.add(tarifa.multiply(costo.iva()).divide(CIEN)));
        long neto = Math.max(0, precioBase - comision - costoTransaccional);
        return new Liquidacion(precioBase, porcentajeComision, comision, costoTransaccional, neto);
    }

    /**
     * El porcentaje vigente en un instante: el de la configuración más reciente que ya empezó. Un cambio posterior no
     * toca los pagos ya confirmados (RF-83).
     */
    public static ConfiguracionComision vigenteEn(Instant instante, Collection<ConfiguracionComision> historial) {
        return historial.stream()
                .filter(configuracion -> !configuracion.vigenteDesde().isAfter(instante))
                .max(Comparator.comparing(ConfiguracionComision::vigenteDesde))
                .orElseThrow(() -> new IllegalStateException("No hay una comisión vigente en " + instante));
    }

    private static long pesos(BigDecimal valor) {
        return valor.setScale(0, RoundingMode.HALF_UP).longValueExact();
    }
}
