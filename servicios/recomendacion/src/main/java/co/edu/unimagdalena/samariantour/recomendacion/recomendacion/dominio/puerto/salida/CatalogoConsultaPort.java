package co.edu.unimagdalena.samariantour.recomendacion.recomendacion.dominio.puerto.salida;

import co.edu.unimagdalena.samariantour.recomendacion.recomendacion.dominio.modelo.OfertaCandidata;
import co.edu.unimagdalena.samariantour.recomendacion.recomendacion.dominio.modelo.ZonaCatalogo;

import java.util.List;

/**
 * Las ofertas visibles del catálogo, la base de conocimiento cerrada de la recomendación (RF-26), y sus zonas.
 */
public interface CatalogoConsultaPort {

    /**
     * Todas las ofertas visibles, con el nombre de su zona.
     *
     * @throws co.edu.unimagdalena.samariantour.recomendacion.recomendacion.dominio.excepcion.RecomendacionNoDisponibleException
     *         si el catálogo no responde y no hay copia en caché
     */
    List<OfertaCandidata> ofertasVisibles();

    /**
     * Las zonas activas y las inactivas que todavía tienen ofertas visibles, con su nombre y su ubicación.
     *
     * @throws co.edu.unimagdalena.samariantour.recomendacion.recomendacion.dominio.excepcion.RecomendacionNoDisponibleException
     *         si el catálogo no responde y no hay copia en caché
     */
    List<ZonaCatalogo> zonas();
}
