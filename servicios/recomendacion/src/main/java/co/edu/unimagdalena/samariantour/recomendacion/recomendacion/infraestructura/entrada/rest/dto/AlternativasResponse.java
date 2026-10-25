package co.edu.unimagdalena.samariantour.recomendacion.recomendacion.infraestructura.entrada.rest.dto;

import co.edu.unimagdalena.samariantour.recomendacion.recomendacion.dominio.modelo.AvisoRecomendacion;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

/**
 * Esquema {@code Alternativas} (RF-29). Cada alternativa tiene la misma forma que una recomendación.
 *
 * @param aplica     false si la zona de la oferta no es de alta afluencia; entonces la lista va vacía y sin zona
 * @param zonaOrigen se omite si no aplica
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record AlternativasResponse(boolean aplica, AvisoRecomendacion aviso, ZonaOrigenResponse zonaOrigen,
                                   List<RecomendacionResponse> alternativas) {
}
