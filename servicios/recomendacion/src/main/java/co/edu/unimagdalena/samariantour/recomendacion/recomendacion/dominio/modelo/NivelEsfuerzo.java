package co.edu.unimagdalena.samariantour.recomendacion.recomendacion.dominio.modelo;

/**
 * Esfuerzo físico, de menor a mayor exigencia: el orden de declaración es el orden de comparación.
 */
public enum NivelEsfuerzo {
    BAJO, MODERADO, ALTA_EXIGENCIA;

    public boolean noSupera(NivelEsfuerzo maximo) {
        return compareTo(maximo) <= 0;
    }
}
