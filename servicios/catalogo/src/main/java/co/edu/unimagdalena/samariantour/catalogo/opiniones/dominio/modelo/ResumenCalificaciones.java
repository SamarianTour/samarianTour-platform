package co.edu.unimagdalena.samariantour.catalogo.opiniones.dominio.modelo;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Optional;

/**
 * Cuántas opiniones aprobadas tiene una oferta y la suma de sus calificaciones (RF-59). El promedio se calcula aquí,
 * y no en la base, para que los dos adaptadores redondeen igual.
 *
 * @param total opiniones aprobadas
 * @param suma  suma de sus calificaciones
 */
public record ResumenCalificaciones(long total, long suma) {

    public static final ResumenCalificaciones SIN_OPINIONES = new ResumenCalificaciones(0, 0);

    public ResumenCalificaciones {
        if (total < 0 || suma < total * Opinion.CALIFICACION_MINIMA || suma > total * Opinion.CALIFICACION_MAXIMA) {
            throw new IllegalArgumentException(
                    "La suma de %d calificaciones no puede ser %d".formatted(total, suma));
        }
    }

    /** Promedio con un decimal, redondeado hacia arriba desde la mitad (4,25 queda 4,3); vacío sin opiniones. */
    public Optional<BigDecimal> promedio() {
        if (total == 0) {
            return Optional.empty();
        }
        return Optional.of(BigDecimal.valueOf(suma).divide(BigDecimal.valueOf(total), 1, RoundingMode.HALF_UP));
    }
}
