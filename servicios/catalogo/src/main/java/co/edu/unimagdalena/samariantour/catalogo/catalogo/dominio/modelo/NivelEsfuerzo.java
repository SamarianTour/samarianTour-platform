package co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo;

/**
 * Nivel de esfuerzo físico de una actividad (RF-17). El orden de exigencia es explícito y no depende del orden
 * de declaración: lo usan el filtro "hasta el nivel indicado" (DC-25) y el nivel de un paquete (ReglasPaquete).
 */
public enum NivelEsfuerzo {
    BAJO(1),
    MODERADO(2),
    ALTA_EXIGENCIA(3);

    private final int exigencia;

    NivelEsfuerzo(int exigencia) {
        this.exigencia = exigencia;
    }

    /** Indica si este nivel es igual o menos exigente que el máximo indicado. */
    public boolean noSupera(NivelEsfuerzo maximo) {
        return exigencia <= maximo.exigencia;
    }

    /** El más exigente de los dos niveles. */
    public static NivelEsfuerzo mayor(NivelEsfuerzo uno, NivelEsfuerzo otro) {
        return uno.exigencia >= otro.exigencia ? uno : otro;
    }
}
