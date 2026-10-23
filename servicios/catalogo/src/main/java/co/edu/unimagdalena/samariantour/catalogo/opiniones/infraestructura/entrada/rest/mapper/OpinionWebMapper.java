package co.edu.unimagdalena.samariantour.catalogo.opiniones.infraestructura.entrada.rest.mapper;

import co.edu.unimagdalena.samariantour.catalogo.opiniones.dominio.modelo.Opinion;
import co.edu.unimagdalena.samariantour.catalogo.opiniones.dominio.modelo.OpinionConOferta;
import co.edu.unimagdalena.samariantour.catalogo.opiniones.dominio.modelo.OpinionesDeOferta;
import co.edu.unimagdalena.samariantour.catalogo.opiniones.dominio.modelo.Pagina;
import co.edu.unimagdalena.samariantour.catalogo.opiniones.dominio.puerto.entrada.comando.PublicarOpinionCommand;
import co.edu.unimagdalena.samariantour.catalogo.opiniones.infraestructura.entrada.rest.dto.MiOpinionResponse;
import co.edu.unimagdalena.samariantour.catalogo.opiniones.infraestructura.entrada.rest.dto.OpinionPublicaResponse;
import co.edu.unimagdalena.samariantour.catalogo.opiniones.infraestructura.entrada.rest.dto.OpinionRequest;
import co.edu.unimagdalena.samariantour.catalogo.opiniones.infraestructura.entrada.rest.dto.OpinionesOfertaResponse;
import co.edu.unimagdalena.samariantour.catalogo.opiniones.infraestructura.entrada.rest.dto.PaginaResponse;
import co.edu.unimagdalena.samariantour.catalogo.opiniones.infraestructura.entrada.rest.dto.ResumenOpinionesResponse;

import java.util.UUID;

/**
 * Convierte las opiniones del turista y la lectura pública (RF-59, RF-60) entre la API y el dominio.
 */
public final class OpinionWebMapper {

    private OpinionWebMapper() {
    }

    /** @param turistaId el de la identidad verificada de la petición */
    public static PublicarOpinionCommand aComando(UUID turistaId, OpinionRequest opinion) {
        return new PublicarOpinionCommand(turistaId, opinion.ofertaId(), opinion.calificacion(), opinion.comentario());
    }

    public static MiOpinionResponse aMiOpinion(OpinionConOferta conOferta) {
        Opinion opinion = conOferta.opinion();
        return new MiOpinionResponse(opinion.id(), opinion.ofertaId(), conOferta.nombreOferta(),
                opinion.calificacion(), opinion.comentario(), opinion.fecha(), opinion.estado(),
                opinion.motivoRechazo(), opinion.revisadaEn());
    }

    public static PaginaResponse<MiOpinionResponse> aPaginaMisOpiniones(Pagina<OpinionConOferta> pagina) {
        return aPaginaResponse(pagina.map(OpinionWebMapper::aMiOpinion));
    }

    public static OpinionesOfertaResponse aOpinionesOferta(OpinionesDeOferta opiniones) {
        Pagina<Opinion> pagina = opiniones.pagina();
        return new OpinionesOfertaResponse(
                new ResumenOpinionesResponse(opiniones.resumen().total(),
                        opiniones.resumen().promedio().orElse(null)),
                pagina.contenido().stream().map(OpinionWebMapper::aPublica).toList(),
                pagina.paginacion().pagina(), pagina.paginacion().tamano(), pagina.totalElementos(),
                pagina.totalPaginas());
    }

    static <T> PaginaResponse<T> aPaginaResponse(Pagina<T> pagina) {
        return new PaginaResponse<>(pagina.contenido(), pagina.paginacion().pagina(), pagina.paginacion().tamano(),
                pagina.totalElementos(), pagina.totalPaginas());
    }

    private static OpinionPublicaResponse aPublica(Opinion opinion) {
        return new OpinionPublicaResponse(opinion.id(), opinion.calificacion(), opinion.comentario(), opinion.fecha());
    }
}
