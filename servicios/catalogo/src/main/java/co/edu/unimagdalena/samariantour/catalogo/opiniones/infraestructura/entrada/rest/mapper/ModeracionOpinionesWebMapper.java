package co.edu.unimagdalena.samariantour.catalogo.opiniones.infraestructura.entrada.rest.mapper;

import co.edu.unimagdalena.samariantour.catalogo.opiniones.dominio.modelo.Opinion;
import co.edu.unimagdalena.samariantour.catalogo.opiniones.dominio.modelo.OpinionConOferta;
import co.edu.unimagdalena.samariantour.catalogo.opiniones.dominio.modelo.Pagina;
import co.edu.unimagdalena.samariantour.catalogo.opiniones.infraestructura.entrada.rest.dto.OpinionEnModeracionResponse;
import co.edu.unimagdalena.samariantour.catalogo.opiniones.infraestructura.entrada.rest.dto.OpinionPendienteResponse;
import co.edu.unimagdalena.samariantour.catalogo.opiniones.infraestructura.entrada.rest.dto.PaginaResponse;

/**
 * Convierte las opiniones en moderación (RF-80) a lo que ven el administrador y el núcleo.
 */
public final class ModeracionOpinionesWebMapper {

    private ModeracionOpinionesWebMapper() {
    }

    public static PaginaResponse<OpinionPendienteResponse> aPaginaPendientes(Pagina<OpinionConOferta> pagina) {
        return OpinionWebMapper.aPaginaResponse(pagina.map(ModeracionOpinionesWebMapper::aPendiente));
    }

    public static OpinionEnModeracionResponse aEnModeracion(Opinion opinion) {
        return new OpinionEnModeracionResponse(opinion.id(), opinion.ofertaId(), opinion.turistaId(),
                opinion.estado(), opinion.fecha());
    }

    private static OpinionPendienteResponse aPendiente(OpinionConOferta conOferta) {
        Opinion opinion = conOferta.opinion();
        return new OpinionPendienteResponse(opinion.id(), opinion.ofertaId(), conOferta.nombreOferta(),
                opinion.calificacion(), opinion.comentario(), opinion.fecha());
    }
}
