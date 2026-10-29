package co.edu.unimagdalena.samariantour.nucleo.administracion.infraestructura.entrada.rest.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Un porcentaje de comisión y desde cuándo rige.
 *
 * @param administradorId quién lo cambió; se omite en el inicial
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record CambioComisionResponse(BigDecimal porcentaje, Instant vigenteDesde, UUID administradorId) {
}
