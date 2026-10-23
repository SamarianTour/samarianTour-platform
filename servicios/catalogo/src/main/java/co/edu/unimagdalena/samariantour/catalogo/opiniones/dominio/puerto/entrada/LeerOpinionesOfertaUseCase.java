package co.edu.unimagdalena.samariantour.catalogo.opiniones.dominio.puerto.entrada;

import co.edu.unimagdalena.samariantour.catalogo.opiniones.dominio.modelo.OpinionesDeOferta;
import co.edu.unimagdalena.samariantour.catalogo.opiniones.dominio.modelo.Paginacion;

/**
 * El turista lee las opiniones aprobadas de una oferta, de la más reciente a la más antigua, con su resumen (RF-59).
 */
public interface LeerOpinionesOfertaUseCase {

    /**
     * @throws co.edu.unimagdalena.samariantour.catalogo.opiniones.dominio.excepcion.OfertaNoEncontradaException si la oferta no es visible, igual que su ficha
     */
    OpinionesDeOferta leer(String ofertaId, Paginacion paginacion);
}
