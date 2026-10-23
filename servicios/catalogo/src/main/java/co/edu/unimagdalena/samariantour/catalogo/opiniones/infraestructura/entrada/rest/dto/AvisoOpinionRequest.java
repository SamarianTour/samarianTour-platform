package co.edu.unimagdalena.samariantour.catalogo.opiniones.infraestructura.entrada.rest.dto;

/**
 * Aviso del núcleo sobre opiniones (DC-04). {@code tipo} decide qué campos aplican.
 *
 * @param tipo      HABILITACION_OPINION, MODERACION_OPINION o SUPRESION_TURISTA
 * @param turistaId HABILITACION_OPINION y SUPRESION_TURISTA
 * @param ofertaId  solo HABILITACION_OPINION
 * @param opinionId solo MODERACION_OPINION
 * @param decision  solo MODERACION_OPINION: APROBAR o RECHAZAR
 * @param motivo    solo MODERACION_OPINION: obligatorio al rechazar
 */
public record AvisoOpinionRequest(
        String idAviso,
        String tipo,
        String turistaId,
        String ofertaId,
        String opinionId,
        String decision,
        String motivo) {
}
