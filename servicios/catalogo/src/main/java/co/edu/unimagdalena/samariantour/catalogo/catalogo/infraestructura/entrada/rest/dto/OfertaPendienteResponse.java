package co.edu.unimagdalena.samariantour.catalogo.catalogo.infraestructura.entrada.rest.dto;

import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.TipoOferta;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.UUID;

/**
 * Una oferta que espera la decisión del administrador (RF-79, DC-38).
 *
 * @param rntPrestadorActivo copia de la habilitación del prestador; el núcleo no aprueba si no está activa
 * @param numeroVersion      número de la versión en moderación; es el que nombra la decisión (DC-05)
 * @param pendiente          la versión en moderación
 * @param publicada          la versión que el turista ve hoy, si la oferta es una edición (RF-66); null si es nueva
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record OfertaPendienteResponse(
        String id,
        TipoOferta tipo,
        UUID prestadorId,
        boolean rntPrestadorActivo,
        int numeroVersion,
        Instant enviadaEn,
        ContenidoVersionResponse pendiente,
        ContenidoVersionResponse publicada) {
}
