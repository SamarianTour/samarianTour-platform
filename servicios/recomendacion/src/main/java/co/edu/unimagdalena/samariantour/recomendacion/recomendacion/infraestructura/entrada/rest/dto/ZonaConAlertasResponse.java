package co.edu.unimagdalena.samariantour.recomendacion.recomendacion.infraestructura.entrada.rest.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

/**
 * Esquema {@code ZonaConAlertas}: una zona con sus alertas activas.
 *
 * @param zonaNombre se omite si la caché del catálogo no lo tiene
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ZonaConAlertasResponse(String zonaId, String zonaNombre, List<AlertaResponse> alertas) {
}
