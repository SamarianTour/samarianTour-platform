package co.edu.unimagdalena.samariantour.catalogo.catalogo.infraestructura.entrada.rest.dto;

/**
 * Aviso que el núcleo envía al catálogo (DC-04). {@code tipo} decide qué campos aplican.
 *
 * @param tipo          MODERACION o HABILITACION
 * @param ofertaId      solo MODERACION
 * @param numeroVersion solo MODERACION: versión que revisó el Administrador
 * @param decision      solo MODERACION: APROBAR o RECHAZAR
 * @param motivo        solo MODERACION: obligatorio al rechazar
 * @param prestadorId   solo HABILITACION
 * @param rntActivo     solo HABILITACION
 * @param version       solo HABILITACION: versión del prestador en el núcleo, que ordena los avisos (DC-02)
 */
public record AvisoRequest(
        String idAviso,
        String tipo,
        String ofertaId,
        Integer numeroVersion,
        String decision,
        String motivo,
        String prestadorId,
        Boolean rntActivo,
        Long version) {
}
