package co.edu.unimagdalena.samariantour.nucleo.indicadores.dominio.modelo;

/** De qué consulta sale una exposición (DR-20 de la recomendación). Solo la recomendación cuenta para el sesgo. */
public enum OrigenExposicion {
    /** La pidió el turista con sus preferencias (RF-26). */
    RECOMENDACION,
    /** Alternativas por alta afluencia (RF-29): no las pide el turista con sus filtros. */
    ALTERNATIVAS
}
