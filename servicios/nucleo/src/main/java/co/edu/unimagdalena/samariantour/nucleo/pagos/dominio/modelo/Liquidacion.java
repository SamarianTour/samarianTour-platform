package co.edu.unimagdalena.samariantour.nucleo.pagos.dominio.modelo;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * Cómo se reparte un pago confirmado (RF-53, RF-54): el precio base, la comisión de la plataforma con el porcentaje
 * aplicado, el costo transaccional de la pasarela y lo que se dispersa al prestador. Todo en pesos enteros.
 */
public record Liquidacion(long montoBruto, BigDecimal porcentajeComision, long comision, long costoTransaccional,
                          long montoNeto) {

    public Liquidacion {
        Objects.requireNonNull(porcentajeComision, "porcentajeComision");
        if (montoBruto <= 0 || comision < 0 || costoTransaccional < 0 || montoNeto < 0) {
            throw new IllegalArgumentException("Montos inválidos en la liquidación");
        }
    }
}
