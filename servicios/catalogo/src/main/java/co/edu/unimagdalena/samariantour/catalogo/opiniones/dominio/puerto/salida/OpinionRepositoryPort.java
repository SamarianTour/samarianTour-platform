package co.edu.unimagdalena.samariantour.catalogo.opiniones.dominio.puerto.salida;

import co.edu.unimagdalena.samariantour.catalogo.opiniones.dominio.modelo.Opinion;
import co.edu.unimagdalena.samariantour.catalogo.opiniones.dominio.modelo.Pagina;
import co.edu.unimagdalena.samariantour.catalogo.opiniones.dominio.modelo.Paginacion;
import co.edu.unimagdalena.samariantour.catalogo.opiniones.dominio.modelo.ResumenCalificaciones;

import java.util.Optional;
import java.util.UUID;

/**
 * Opiniones de los turistas (colección {@code opiniones}, DC-46).
 */
public interface OpinionRepositoryPort {

    /** Identificador para una opinión nueva, con formato ObjectId (DC-30). */
    String nuevoId();

    /** Vacía si no existe o si el id no tiene formato ObjectId. */
    Optional<Opinion> buscarPorId(String id);

    /**
     * Inserta una opinión nueva (revisión 0) o reemplaza una leída, solo si su revisión no cambió desde la lectura
     * (ADR-05). Queda con la revisión siguiente.
     *
     * @throws co.edu.unimagdalena.samariantour.catalogo.opiniones.dominio.excepcion.OpinionDuplicadaException si es nueva y el turista ya opinó sobre esa oferta (RF-61)
     * @throws co.edu.unimagdalena.samariantour.catalogo.compartido.concurrencia.ConflictoConcurrenciaException si otra escritura la cambió o la borró
     */
    void guardar(Opinion opinion);

    /** Si el turista ya opinó sobre la oferta, en cualquier estado (RF-61). */
    boolean existeDe(UUID turistaId, String ofertaId);

    /** Opiniones aprobadas de la oferta, de la más reciente a la más antigua y luego por id (RF-59). */
    Pagina<Opinion> buscarAprobadas(String ofertaId, Paginacion paginacion);

    /** Cuántas opiniones aprobadas tiene la oferta y la suma de sus calificaciones. */
    ResumenCalificaciones resumirAprobadas(String ofertaId);

    /** Opiniones del turista en cualquier estado, de la más reciente a la más antigua y luego por id. */
    Pagina<Opinion> buscarDelTurista(UUID turistaId, Paginacion paginacion);

    /** Opiniones en moderación, de la enviada hace más tiempo a la más reciente y luego por id (RF-80). */
    Pagina<Opinion> buscarEnModeracion(Paginacion paginacion);

    /**
     * Borra todas las opiniones del turista (DC-45).
     *
     * @return cuántas borró
     */
    long eliminarDelTurista(UUID turistaId);
}
