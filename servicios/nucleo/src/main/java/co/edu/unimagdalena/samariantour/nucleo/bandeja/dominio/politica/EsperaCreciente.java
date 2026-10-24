package co.edu.unimagdalena.samariantour.nucleo.bandeja.dominio.politica;

import java.time.Duration;

/**
 * Cuánto esperar antes de reintentar un aviso que no llegó (DN-06): 5 s tras el primer fallo y el doble tras cada
 * uno de los siguientes, hasta 5 minutos. Se reintenta sin límite: si el catálogo pasa un rato caído, ningún aviso
 * se pierde.
 */
public final class EsperaCreciente {

    public static final Duration PRIMERA = Duration.ofSeconds(5);
    public static final Duration MAXIMA = Duration.ofMinutes(5);

    private EsperaCreciente() {
    }

    /** @param fallos los intentos fallidos hasta ahora, contando el último; al menos 1 */
    public static Duration despuesDe(int fallos) {
        if (fallos < 1) {
            throw new IllegalArgumentException("Después de al menos un fallo");
        }
        // A partir del séptimo fallo la espera ya pasó el máximo; así el desplazamiento nunca desborda.
        Duration espera = PRIMERA.multipliedBy(1L << Math.min(fallos - 1, 7));
        return espera.compareTo(MAXIMA) > 0 ? MAXIMA : espera;
    }
}
