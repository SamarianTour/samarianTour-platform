package co.edu.unimagdalena.samariantour.recomendacion.recomendacion.dominio.puerto.salida;

import co.edu.unimagdalena.samariantour.recomendacion.recomendacion.dominio.modelo.MotorRecomendacion;
import co.edu.unimagdalena.samariantour.recomendacion.recomendacion.dominio.modelo.OfertaEvaluada;
import co.edu.unimagdalena.samariantour.recomendacion.recomendacion.dominio.modelo.Preferencias;
import co.edu.unimagdalena.samariantour.recomendacion.recomendacion.dominio.modelo.Recomendacion;

import java.util.List;

/**
 * Un motor de recomendación (RF-26, RF-27, QAS-06): por reglas o con un proveedor de IA. Cuál responde se decide por
 * configuración, sin tocar código (DR-01).
 */
public interface RecommendationPort {

    MotorRecomendacion motor();

    /**
     * Elige y explica de 3 a 5 ofertas, o todas si hay menos de 3.
     *
     * @param candidatas las que pasaron los filtros duros, de mayor a menor afinidad y rotadas entre empates; nunca
     *                   vacía
     * @throws co.edu.unimagdalena.samariantour.recomendacion.recomendacion.dominio.excepcion.MotorNoDisponibleException
     *         si el motor no pudo responder
     */
    List<Recomendacion> recomendar(Preferencias preferencias, List<OfertaEvaluada> candidatas);
}
