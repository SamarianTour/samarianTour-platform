package co.edu.unimagdalena.samariantour.recomendacion.recomendacion.dominio.modelo;

/**
 * Aviso que acompaña a las recomendaciones.
 * <p>{@code POCAS_COINCIDENCIAS} [SUPUESTO] (DR-03): menos de 3 ofertas pasan los filtros duros. Se devuelven las que
 * haya, sin relajar los filtros: a quien pidió accesibilidad nunca se le recomienda algo no accesible.
 */
public enum AvisoRecomendacion {
    POCAS_COINCIDENCIAS
}
