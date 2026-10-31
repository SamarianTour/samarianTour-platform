package co.edu.unimagdalena.samariantour.nucleo.pagos.dominio.politica;

import java.time.Duration;
import java.time.Instant;

/**
 * Cuántas veces se intenta cada cosa (DN-20, DN-21, DN-24), en un solo lugar como las ventanas de las solicitudes
 * (QAS-05):
 * <ul>
 *   <li>tres intentos de pago por solicitud: el primero y dos reintentos (RF-58);</li>
 *   <li>un intento sin resultado 30 min después de abierto queda abandonado [SUPUESTO]; si la ventana de la solicitud
 *   vence mientras está en curso, al terminar su gracia de 30 min (RF-55);</li>
 *   <li>tres reintentos de la dispersión después del primer envío, con espera creciente (RF-54);</li>
 *   <li>un evento de la pasarela que falla al procesarse se reintenta con espera creciente y, al quinto fallo, queda
 *   fallido para revisarlo a mano [SUPUESTO].</li>
 * </ul>
 */
public final class PoliticaReintentos {

    public static final int INTENTOS_PAGO = 3;
    public static final Duration ABANDONO_INTENTO = Duration.ofMinutes(30);
    /** Lo que la solicitud aplaza su vencimiento mientras hay un intento en curso (RF-55). */
    public static final Duration GRACIA_PAGO = Duration.ofMinutes(30);
    public static final int REINTENTOS_DISPERSION = 3;
    public static final int FALLOS_EVENTO = 5;
    /** Como la bandeja (DN-06): 5 s tras el primer fallo y el doble tras cada uno, hasta 5 minutos. */
    public static final Duration PRIMERA_ESPERA_EVENTO = Duration.ofSeconds(5);
    public static final Duration MAXIMA_ESPERA_EVENTO = Duration.ofMinutes(5);
    /**
     * Cuánto se aplaza un abandono o un aviso que falló por un error inesperado [SUPUESTO], para que no se tome de
     * primero en cada corrida y no detenga a los demás.
     */
    public static final Duration ESPERA_TRAS_ERROR = Duration.ofMinutes(1);

    private PoliticaReintentos() {
    }

    /** Si la solicitud todavía admite otro intento: cuentan solo los que tuvieron resultado de la pasarela. */
    public static boolean admiteOtroIntento(long intentosConResultado) {
        return intentosConResultado < INTENTOS_PAGO;
    }

    /**
     * Cuándo se abandona un intento sin resultado (DN-20): a los 30 min de abierto, salvo que la ventana de la solicitud
     * venza antes; en ese caso, al terminar la gracia de RF-55 (el fin de la ventana más 30 min), nunca después. Así la
     * solicitud no vence antes de su gracia, y un aprobado que llega dentro de ella la paga.
     *
     * @param venceEn el fin de la ventana de la solicitud al abrir el intento; vacío si no se conoce
     */
    public static Instant abandonoDe(Instant abiertoEn, Instant venceEn) {
        Instant aLosTreintaMinutos = abiertoEn.plus(ABANDONO_INTENTO);
        if (venceEn == null || !venceEn.isBefore(aLosTreintaMinutos)) {
            return aLosTreintaMinutos;
        }
        return venceEn.plus(GRACIA_PAGO);
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

    /**
     * La espera antes de reintentar un evento que falló (DN-21): 5, 10, 20 y 40 s tras los cuatro primeros fallos.
     *
     * @param fallos los fallos hasta ahora, contando el último; al menos 1
     */
    public static Duration esperaEvento(int fallos) {
        if (fallos < 1) {
            throw new IllegalArgumentException("Después de al menos un fallo");
        }
        Duration espera = PRIMERA_ESPERA_EVENTO.multipliedBy(1L << Math.min(fallos - 1, 7));
        return espera.compareTo(MAXIMA_ESPERA_EVENTO) > 0 ? MAXIMA_ESPERA_EVENTO : espera;
    }

    /** Si un evento con esos fallos ya no se reintenta y queda para revisión manual. */
    public static boolean eventoFallido(int fallos) {
        return fallos >= FALLOS_EVENTO;
    }
}
