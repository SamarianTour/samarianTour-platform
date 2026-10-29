package co.edu.unimagdalena.samariantour.nucleo.pagos.api;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Un porcentaje de comisión y desde cuándo rige (RF-82).
 *
 * @param administradorId quién lo cambió; vacío en el inicial
 */
public record ComisionConfigurada(BigDecimal porcentaje, Instant vigenteDesde, UUID administradorId) {
}
