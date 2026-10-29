package co.edu.unimagdalena.samariantour.nucleo.pagos.dominio.modelo;

import co.edu.unimagdalena.samariantour.nucleo.pagos.dominio.excepcion.DatoInvalidoException;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * El porcentaje de comisión de la plataforma desde una fecha (RF-82, RF-83, DN-23). Cada cambio agrega uno nuevo: el
 * historial no se borra y un pago ya confirmado guarda el porcentaje que se le aplicó.
 *
 * @param porcentaje      sobre el precio base, de 0 a menos de 100, con hasta dos decimales [SUPUESTO]
 * @param administradorId quién lo cambió; vacío en el inicial
 */
public record ConfiguracionComision(BigDecimal porcentaje, Instant vigenteDesde, UUID administradorId) {

    /** El inicial, ratificado por el equipo (SRS, anexo 5.4, RF-53). */
    public static final BigDecimal PORCENTAJE_INICIAL = new BigDecimal("4.00");

    public ConfiguracionComision {
        Objects.requireNonNull(vigenteDesde, "vigenteDesde");
        if (porcentaje == null) {
            throw new DatoInvalidoException("porcentaje", "es obligatorio");
        }
        if (porcentaje.signum() < 0 || porcentaje.compareTo(new BigDecimal("100")) >= 0) {
            throw new DatoInvalidoException("porcentaje", "debe estar entre 0 y menos de 100");
        }
        if (porcentaje.stripTrailingZeros().scale() > 2) {
            throw new DatoInvalidoException("porcentaje", "admite hasta dos decimales");
        }
        porcentaje = porcentaje.setScale(2);
    }
}
