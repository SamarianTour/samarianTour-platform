package co.edu.unimagdalena.samariantour.nucleo.pagos.dominio.politica;

import java.time.Duration;

/**
 * Cuántas veces se intenta cada cosa (DN-20, DN-24), en un solo lugar como las ventanas de las solicitudes (QAS-05):
 * <ul>
 *   <li>tres intentos de pago por solicitud: el primero y dos reintentos (RF-58);</li>
 *   <li>un intento sin resultado 30 min después de abierto queda abandonado [SUPUESTO], el mismo máximo que aplaza el
 *   vencimiento (RF-55);</li>
 *   <li>tres reintentos de la dispersión después del primer envío, con espera creciente (RF-54).</li>
 * </ul>
 */
public final class PoliticaReintentos {

    public static final int INTENTOS_PAGO = 3;
    public static final Duration ABANDONO_INTENTO = Duration.ofMinutes(30);
    public static final int REINTENTOS_DISPERSION = 3;

    private PoliticaReintentos() {
    }

    /** Si la solicitud todavía admite otro intento: cuentan solo los que tuvieron resultado de la pasarela. */
    public static boolean admiteOtroIntento(long intentosConResultado) {
        return intentosConResultado < INTENTOS_PAGO;
    }

    /**
     * La espera antes del próximo intento de una dispersión [SUPUESTO]: la base tras el primer fallo y el doble tras
     * cada uno de los siguientes (con 1 min de base: 1, 2 y 4 min).
     *
     * @param fallos los fallos hasta ahora, contando el último; al menos 1
     */
    public static Duration esperaDispersion(int fallos, Duration base) {
        if (fallos < 1) {
            throw new IllegalArgumentException("Después de al menos un fallo");
        }
        return base.multipliedBy(1L << Math.min(fallos - 1, 10));
    }
}
