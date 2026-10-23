package co.edu.unimagdalena.samariantour.catalogo.opiniones.infraestructura.entrada.rest.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.math.BigDecimal;

/**
 * Resumen de las opiniones aprobadas de una oferta (RF-59, DC-42).
 *
 * @param promedioCalificacion con un decimal; no se envía si la oferta no tiene opiniones aprobadas
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ResumenOpinionesResponse(long totalOpiniones, BigDecimal promedioCalificacion) {
}
