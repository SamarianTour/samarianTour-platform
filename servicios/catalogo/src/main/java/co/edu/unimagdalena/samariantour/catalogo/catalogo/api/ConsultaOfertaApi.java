package co.edu.unimagdalena.samariantour.catalogo.catalogo.api;

import java.util.Collection;
import java.util.Map;

/**
 * Lo que otro módulo puede preguntar de una oferta sin conocer el modelo del catálogo. Hoy la usa el módulo de
 * opiniones (DC-39).
 */
public interface ConsultaOfertaApi {

    /** Si la oferta existe, en cualquier estado: retirada o no, visible o no. */
    boolean existe(String ofertaId);

    /** Si el turista puede verla, con la misma regla que la ficha (RF-22, DC-35). */
    boolean esVisible(String ofertaId);

    /**
     * El nombre de cada oferta que existe, por id: el de la versión publicada o, si todavía no tiene, el de la
     * pendiente. Omite las ofertas que no existen.
     */
    Map<String, String> nombres(Collection<String> ofertaIds);
}
