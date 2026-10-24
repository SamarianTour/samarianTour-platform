package co.edu.unimagdalena.samariantour.catalogo.catalogo.infraestructura.entrada.rest.dto;

import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.TipoOferta;
import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Una oferta propia completa, para que el prestador la revise o la corrija (DC-50). A diferencia de la lista de
 * ofertas propias, trae todo el contenido de cada versión.
 *
 * @param publicada la versión que ve el turista; vacía si la oferta nunca se aprobó
 * @param pendiente la versión en moderación o rechazada; vacía si no hay
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record OfertaPropiaResponse(
        String id,
        TipoOferta tipo,
        boolean retirada,
        VersionPropiaResponse publicada,
        VersionPropiaResponse pendiente) {
}
