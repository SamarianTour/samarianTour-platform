package co.edu.unimagdalena.samariantour.recomendacion.recomendacion.dominio.modelo;

/**
 * Criterios explícitos con los que se elige y se explica una recomendación (RF-27, DR-03). Son los únicos: nunca
 * calificaciones, opiniones, comisión, tamaño ni identidad del prestador.
 * <p>Los blandos suman afinidad; los duros son filtros que la oferta tiene que cumplir si el turista los pidió.
 */
public enum CriterioAfinidad {
    TIPO_OFERTA(true),
    TIPO_ACTIVIDAD(true),
    ZONA(true),
    ACCESIBILIDAD(false),
    NIVEL_ESFUERZO(false),
    PRECIO(false);

    private final boolean blando;

    CriterioAfinidad(boolean blando) {
        this.blando = blando;
    }

    public boolean esBlando() {
        return blando;
    }
}
