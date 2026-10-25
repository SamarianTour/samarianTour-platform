package co.edu.unimagdalena.samariantour.recomendacion.recomendacion.infraestructura.entrada.rest.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Esquema {@code ZonaOrigen}: la zona con alta afluencia de la oferta consultada.
 *
 * @param nombre se omite si el catálogo no lo pudo dar
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ZonaOrigenResponse(String id, String nombre) {
}
