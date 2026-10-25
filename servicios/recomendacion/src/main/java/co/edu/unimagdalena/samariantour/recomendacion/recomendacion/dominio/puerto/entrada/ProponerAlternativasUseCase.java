package co.edu.unimagdalena.samariantour.recomendacion.recomendacion.dominio.puerto.entrada;

import co.edu.unimagdalena.samariantour.recomendacion.recomendacion.dominio.modelo.Idioma;
import co.edu.unimagdalena.samariantour.recomendacion.recomendacion.dominio.modelo.ResultadoAlternativas;

/**
 * Propone ofertas del mismo tipo en zonas sin alta afluencia cuando la oferta está en una zona con alta afluencia
 * (RF-29).
 */
public interface ProponerAlternativasUseCase {

    /**
     * @throws co.edu.unimagdalena.samariantour.recomendacion.recomendacion.dominio.excepcion.OfertaNoEncontradaException
     *         si la oferta no está entre las visibles del catálogo
     * @throws co.edu.unimagdalena.samariantour.recomendacion.recomendacion.dominio.excepcion.RecomendacionNoDisponibleException
     *         si el catálogo no responde y no hay copia en caché
     */
    ResultadoAlternativas proponer(String ofertaId, Idioma idioma);
}
