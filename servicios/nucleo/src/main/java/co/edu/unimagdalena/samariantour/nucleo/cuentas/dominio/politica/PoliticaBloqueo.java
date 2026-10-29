package co.edu.unimagdalena.samariantour.nucleo.cuentas.dominio.politica;

import java.time.Duration;
import java.time.Instant;

/** Cinco inicios de sesión fallidos consecutivos bloquean la cuenta durante 15 minutos (RF-05). */
public final class PoliticaBloqueo {

    public static final int INTENTOS_MAXIMOS = 5;
    public static final Duration DURACION = Duration.ofMinutes(15);

    private PoliticaBloqueo() {
    }

    public static boolean debeBloquear(int fallidosConsecutivos) {
        return fallidosConsecutivos >= INTENTOS_MAXIMOS;
    }

    public static Instant bloqueadaHasta(Instant ahora) {
        return ahora.plus(DURACION);
    }
}
