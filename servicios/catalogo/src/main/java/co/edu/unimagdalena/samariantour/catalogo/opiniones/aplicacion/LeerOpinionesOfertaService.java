package co.edu.unimagdalena.samariantour.catalogo.opiniones.aplicacion;

import co.edu.unimagdalena.samariantour.catalogo.opiniones.dominio.excepcion.OfertaNoEncontradaException;
import co.edu.unimagdalena.samariantour.catalogo.opiniones.dominio.modelo.OpinionesDeOferta;
import co.edu.unimagdalena.samariantour.catalogo.opiniones.dominio.modelo.Paginacion;
import co.edu.unimagdalena.samariantour.catalogo.opiniones.dominio.puerto.entrada.LeerOpinionesOfertaUseCase;
import co.edu.unimagdalena.samariantour.catalogo.opiniones.dominio.puerto.salida.OfertaCatalogoPort;
import co.edu.unimagdalena.samariantour.catalogo.opiniones.dominio.puerto.salida.OpinionRepositoryPort;

/**
 * Lectura pública de las opiniones de una oferta (RF-59, DC-43): solo las aprobadas, con el resumen de todas ellas.
 * Una oferta que el turista no puede ver en el catálogo tampoco muestra sus opiniones.
 */
public class LeerOpinionesOfertaService implements LeerOpinionesOfertaUseCase {

    private final OpinionRepositoryPort opiniones;
    private final OfertaCatalogoPort ofertas;

    public LeerOpinionesOfertaService(OpinionRepositoryPort opiniones, OfertaCatalogoPort ofertas) {
        this.opiniones = opiniones;
        this.ofertas = ofertas;
    }

    @Override
    public OpinionesDeOferta leer(String ofertaId, Paginacion paginacion) {
        if (!ofertas.esVisible(ofertaId)) {
            throw new OfertaNoEncontradaException(ofertaId);
        }
        return new OpinionesDeOferta(opiniones.buscarAprobadas(ofertaId, paginacion),
                opiniones.resumirAprobadas(ofertaId));
    }
}
