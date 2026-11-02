package co.edu.unimagdalena.samariantour.nucleo.indicadores.dominio.politica;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Solo datos agregados (RET-03): un conteo de 1 o 2 no se publica, porque con una zona, un tipo y un periodo cortos
 * podría señalar a un turista; la pantalla muestra "menos de 3" [SUPUESTO] (DN-41). El 0 sí se publica: no señala a
 * nadie.
 */
public final class ReglaAnonimizacion {

    public static final int MINIMO = 3;

    private ReglaAnonimizacion() {
    }

    /** @return el conteo, o vacío si es 1 o 2 */
    public static Long publicable(long conteo) {
        return conteo > 0 && conteo < MINIMO ? null : conteo;
    }

    /**
     * El porcentaje de {@code parte} sobre {@code total}, con un decimal. Vacío si la parte no se publica, porque el
     * porcentaje la delataría, o si no hay total.
     */
    public static BigDecimal porcentaje(long parte, long total) {
        if (publicable(parte) == null || total == 0) {
            return null;
        }
        return BigDecimal.valueOf(parte * 100).divide(BigDecimal.valueOf(total), 1, RoundingMode.HALF_UP);
    }
}
