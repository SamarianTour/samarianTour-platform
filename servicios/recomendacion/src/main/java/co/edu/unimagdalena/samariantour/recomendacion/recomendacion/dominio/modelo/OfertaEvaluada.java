package co.edu.unimagdalena.samariantour.recomendacion.recomendacion.dominio.modelo;

import java.util.Set;

/**
 * Una oferta que pasó los filtros duros, con su afinidad y los criterios que cumple (DR-03).
 *
 * @param criteriosCumplidos los blandos que coinciden y los duros que el turista pidió y la oferta cumple
 * @param afinidad           un punto por cada criterio blando que coincide
 */
public record OfertaEvaluada(OfertaCandidata oferta, Set<CriterioAfinidad> criteriosCumplidos, int afinidad) {

    public OfertaEvaluada {
        criteriosCumplidos = Preferencias.copia(criteriosCumplidos, CriterioAfinidad.class);
    }
}
