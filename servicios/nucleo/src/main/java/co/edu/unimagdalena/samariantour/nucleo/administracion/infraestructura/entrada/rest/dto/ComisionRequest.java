package co.edu.unimagdalena.samariantour.nucleo.administracion.infraestructura.entrada.rest.dto;

import java.math.BigDecimal;

/**
 * El nuevo porcentaje de comisión de la plataforma (RF-82).
 *
 * @param porcentaje sobre el precio base, de 0 a menos de 100, con hasta dos decimales [SUPUESTO]
 */
public record ComisionRequest(BigDecimal porcentaje) {
}
