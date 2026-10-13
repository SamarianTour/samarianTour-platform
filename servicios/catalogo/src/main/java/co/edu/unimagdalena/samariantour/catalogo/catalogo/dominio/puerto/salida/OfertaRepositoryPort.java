package co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.puerto.salida;

import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.CriteriosBusqueda;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.Oferta;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.Pagina;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.Paginacion;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Almacenamiento de ofertas (RF-14 a RF-22, QAS-07).
 */
public interface OfertaRepositoryPort {

    /**
     * Identificador para una oferta nueva. Lo genera la persistencia, no la aplicación: en MongoDB es un ObjectId
     * (DC-30). Para el dominio es un texto opaco.
     */
    String nuevoId();

    Optional<Oferta> buscarPorId(String id);

    /**
     * Ofertas con esos identificadores, sin orden garantizado; omite los que no existen. Sirve para cargar las
     * actividades de un paquete (RF-22). Un adaptador puede reemplazarlo por una sola consulta.
     */
    default List<Oferta> buscarPorIds(Collection<String> ids) {
        return ids.stream().map(this::buscarPorId).flatMap(Optional::stream).toList();
    }

    /** Crea la oferta o reemplaza la guardada con el mismo id. */
    void guardar(Oferta oferta);

    /**
     * Ofertas visibles que cumplen los criterios, ordenadas por precio de referencia de la versión publicada
     * y luego por id. La visibilidad es la de ReglaVisibilidad; un paquete además exige que todas sus actividades
     * sean visibles, y sus tipos de actividad, su nivel de esfuerzo y su accesibilidad se calculan con ellas
     * (ReglasPaquete, RF-17, RF-22).
     */
    Pagina<Oferta> buscarVisibles(CriteriosBusqueda criterios);

    /** Ofertas del prestador en cualquier estado, ordenadas por id. */
    Pagina<Oferta> buscarPorPrestador(UUID prestadorId, Paginacion paginacion);

    /** Actualiza la copia de la habilitación RNT en todas las ofertas del prestador. */
    void actualizarRntPrestador(UUID prestadorId, boolean rntActivo);
}
