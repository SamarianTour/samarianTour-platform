package co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.puerto.salida;

import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.CriteriosBusqueda;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.Oferta;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.Pagina;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.Paginacion;

import java.util.Optional;
import java.util.UUID;

/**
 * Almacenamiento de ofertas (RF-14 a RF-22, QAS-07).
 */
public interface OfertaRepositoryPort {

    Optional<Oferta> buscarPorId(String id);

    /** Crea la oferta o reemplaza la guardada con el mismo id. */
    void guardar(Oferta oferta);

    /**
     * Ofertas visibles que cumplen los criterios (ReglaVisibilidad), ordenadas por precio de referencia de la
     * versión publicada y luego por id.
     */
    Pagina<Oferta> buscarVisibles(CriteriosBusqueda criterios);

    /** Ofertas del prestador en cualquier estado, ordenadas por id. */
    Pagina<Oferta> buscarPorPrestador(UUID prestadorId, Paginacion paginacion);

    /** Actualiza la copia de la habilitación RNT en todas las ofertas del prestador. */
    void actualizarRntPrestador(UUID prestadorId, boolean rntActivo);
}
