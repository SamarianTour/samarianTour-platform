package co.edu.unimagdalena.samariantour.nucleo.prestadores.dominio.politica;

import java.time.Duration;
import java.util.random.RandomGenerator;

/**
 * El código que verifica el WhatsApp del prestador (RF-71, DN-33): las mismas reglas que el del turista en las cuentas
 * (DN-28), 6 dígitos, 30 minutos (RF-08) y 3 intentos. Cada módulo tiene las suyas: no comparten dominio.
 */
public final class PoliticaCodigoWhatsapp {

    public static final Duration VIGENCIA = Duration.ofMinutes(30);
    public static final int INTENTOS_MAXIMOS = 3;

    private PoliticaCodigoWhatsapp() {
    }

    public static String nuevoCodigo(RandomGenerator azar) {
        return "%06d".formatted(azar.nextInt(1_000_000));
    }
}
