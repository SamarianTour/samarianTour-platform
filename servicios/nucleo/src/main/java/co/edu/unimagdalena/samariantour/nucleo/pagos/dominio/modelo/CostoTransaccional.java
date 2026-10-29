package co.edu.unimagdalena.samariantour.nucleo.pagos.dominio.modelo;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * Lo que cobra la pasarela por una transacción aprobada (RF-53, DN-23). El real depende del medio de pago y del
 * contrato del comercio, y Wompi no lo informa en sus eventos: en el prototipo es un parámetro de configuración
 * [SUPUESTO], con la tarifa publicada del Plan Avanzado de Wompi como valor por defecto (2,65 % + $700 + IVA del 19 %).
 * En el ambiente real se toma del reporte de la pasarela.
 *
 * @param porcentaje sobre el precio base
 * @param fijo       en pesos, por transacción
 * @param iva        porcentaje del IVA sobre la tarifa; 0 si no aplica
 */
public record CostoTransaccional(BigDecimal porcentaje, long fijo, BigDecimal iva) {

    public CostoTransaccional {
        Objects.requireNonNull(porcentaje, "porcentaje");
        Objects.requireNonNull(iva, "iva");
        if (porcentaje.signum() < 0 || fijo < 0 || iva.signum() < 0) {
            throw new IllegalArgumentException("El costo transaccional no puede ser negativo");
        }
    }

    public static CostoTransaccional ninguno() {
        return new CostoTransaccional(BigDecimal.ZERO, 0, BigDecimal.ZERO);
    }
}
