package co.edu.unimagdalena.samariantour.recomendacion.recomendacion.dominio.puerto.entrada;

import co.edu.unimagdalena.samariantour.recomendacion.recomendacion.dominio.modelo.Preferencias;
import co.edu.unimagdalena.samariantour.recomendacion.recomendacion.dominio.modelo.ResultadoRecomendacion;

/**
 * Recomienda de 3 a 5 ofertas visibles del catálogo, cada una con la explicación del criterio usado (RF-26, RF-27).
 */
public interface RecomendarOfertasUseCase {

    /**
     * @throws co.edu.unimagdalena.samariantour.recomendacion.recomendacion.dominio.excepcion.RecomendacionNoDisponibleException
     *         si el catálogo no responde y no hay copia en caché
     */
    ResultadoRecomendacion recomendar(Preferencias preferencias);
}
