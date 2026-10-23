package co.edu.unimagdalena.samariantour.catalogo.opiniones.dominio.puerto.salida;

import co.edu.unimagdalena.samariantour.catalogo.opiniones.dominio.modelo.HabilitacionOpinion;

import java.util.UUID;

/**
 * Turistas que pueden opinar sobre una oferta porque la pagaron (colección {@code habilitaciones_opinion}, DC-40).
 * Hay a lo sumo una habilitación por turista y oferta.
 */
public interface HabilitacionOpinionRepositoryPort {

    /**
     * Registra la habilitación. Debe tolerar que ya exista, por un aviso repetido o porque otra réplica la registró
     * al mismo tiempo.
     *
     * @return true si la registró; false si ya existía
     */
    boolean registrar(HabilitacionOpinion habilitacion);

    boolean existe(UUID turistaId, String ofertaId);

    /**
     * Borra todas las habilitaciones del turista (DC-44).
     *
     * @return cuántas borró
     */
    long eliminarDelTurista(UUID turistaId);
}
