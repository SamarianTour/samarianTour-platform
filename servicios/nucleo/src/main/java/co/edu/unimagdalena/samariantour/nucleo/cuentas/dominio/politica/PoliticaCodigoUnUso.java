package co.edu.unimagdalena.samariantour.nucleo.cuentas.dominio.politica;

import java.time.Duration;
import java.time.Instant;
import java.util.random.RandomGenerator;

/**
 * Un código o enlace de un solo uso vence a los 30 minutos de emitido o al usarse (RF-08). Tres intentos con un
 * código equivocado también lo invalidan [SUPUESTO] (DN-28): un código de 6 dígitos no se puede adivinar en tres.
 */
public final class PoliticaCodigoUnUso {

    public static final Duration VIGENCIA = Duration.ofMinutes(30);
    public static final int INTENTOS_MAXIMOS = 3;

    private PoliticaCodigoUnUso() {
    }

    /** Un código de 6 dígitos, con ceros a la izquierda si hace falta. */
    public static String nuevoCodigo(RandomGenerator azar) {
        return "%06d".formatted(azar.nextInt(1_000_000));
    }

    public static Instant expiraEn(Instant emitidoEn) {
        return emitidoEn.plus(VIGENCIA);
    }
}
