package co.edu.unimagdalena.samariantour.catalogo.opiniones.dominio.puerto.salida;

import java.util.Collection;
import java.util.Map;

/**
 * Lo que el módulo de opiniones necesita saber de las ofertas, que son del módulo catálogo. El adaptador lo pregunta
 * por {@code catalogo.api}, la única parte del catálogo que este módulo puede usar (DC-40).
 */
public interface OfertaCatalogoPort {

    /** Si la oferta existe, en cualquier estado: retirada o no, visible o no. */
    boolean existe(String ofertaId);

    /** Si el turista puede verla en el catálogo, con la misma regla que la ficha (RF-22, DC-35). */
    boolean esVisible(String ofertaId);

    /** El nombre de cada oferta que existe, por id; omite las que no existen. */
    Map<String, String> nombres(Collection<String> ofertaIds);
}
