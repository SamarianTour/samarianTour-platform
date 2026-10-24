package co.edu.unimagdalena.samariantour.nucleo.administracion.dominio.puerto.salida;

import co.edu.unimagdalena.samariantour.nucleo.administracion.dominio.excepcion.CatalogoNoDisponibleException;
import co.edu.unimagdalena.samariantour.nucleo.administracion.dominio.modelo.OfertaEnModeracion;
import co.edu.unimagdalena.samariantour.nucleo.administracion.dominio.modelo.OpinionEnModeracion;

import java.util.Optional;

/** Las consultas internas del catálogo sobre lo que está en moderación (DC-38 y DC-44 del catálogo). */
public interface ModeracionCatalogoPort {

    /**
     * @return vacío si la oferta no existe, está retirada o no tiene una versión en moderación
     * @throws CatalogoNoDisponibleException si el catálogo no responde a tiempo
     */
    Optional<OfertaEnModeracion> ofertaEnModeracion(String ofertaId);

    /**
     * @return vacío si la opinión no existe o ya no está en moderación
     * @throws CatalogoNoDisponibleException si el catálogo no responde a tiempo
     */
    Optional<OpinionEnModeracion> opinionEnModeracion(String opinionId);
}
