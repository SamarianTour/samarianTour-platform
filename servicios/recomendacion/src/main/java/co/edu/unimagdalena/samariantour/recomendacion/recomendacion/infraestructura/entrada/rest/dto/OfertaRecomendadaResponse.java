package co.edu.unimagdalena.samariantour.recomendacion.recomendacion.infraestructura.entrada.rest.dto;

import co.edu.unimagdalena.samariantour.recomendacion.recomendacion.dominio.modelo.TipoOferta;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.math.BigDecimal;

/**
 * Esquema {@code OfertaRecomendada}: lo necesario para la tarjeta y el enlace a la ficha. No incluye al prestador.
 *
 * @param zonaNombre se omite si el catálogo no lo pudo dar
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record OfertaRecomendadaResponse(String id, String nombre, TipoOferta tipo, String zonaNombre,
                                        BigDecimal precioReferencia, String unidadPrecio) {
}
