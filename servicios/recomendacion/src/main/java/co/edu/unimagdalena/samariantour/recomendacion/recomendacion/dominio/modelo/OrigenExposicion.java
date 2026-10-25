package co.edu.unimagdalena.samariantour.recomendacion.recomendacion.dominio.modelo;

/**
 * De qué consulta sale una exposición (RET-02, DR-20): la recomendación por preferencias (RF-27) o las alternativas
 * por alta afluencia (RF-29). La métrica de sesgo las separa: las alternativas no las pide el turista con sus filtros.
 */
public enum OrigenExposicion {
    RECOMENDACION, ALTERNATIVAS
}
