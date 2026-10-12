package co.edu.unimagdalena.samariantour.catalogo.catalogo.infraestructura.entrada.rest.dto;

import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.TipoOferta;
import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Una oferta como la ve su prestador: la versión publicada, que ve el turista, y la pendiente de moderación (RF-66).
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record OfertaPrestadorResponse(
        String id,
        TipoOferta tipo,
        boolean retirada,
        VersionResponse publicada,
        VersionResponse pendiente) {
}
