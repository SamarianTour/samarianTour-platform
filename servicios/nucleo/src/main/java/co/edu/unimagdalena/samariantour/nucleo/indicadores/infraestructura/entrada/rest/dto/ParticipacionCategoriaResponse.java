package co.edu.unimagdalena.samariantour.nucleo.indicadores.infraestructura.entrada.rest.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.math.BigDecimal;

/**
 * @param categoria   GUIA_INDEPENDIENTE, MICRO_PEQUENO, MEDIANO, GRANDE o SIN_CATEGORIA
 * @param solicitudes se omite si es 1 o 2 (RET-03)
 * @param porcentaje  sobre el total; se omite con el conteo
 * @param pagadas     se omite si es 1 o 2
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ParticipacionCategoriaResponse(String categoria, Long solicitudes, BigDecimal porcentaje,
                                             Long pagadas) {
}
