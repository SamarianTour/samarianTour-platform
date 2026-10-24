package co.edu.unimagdalena.samariantour.nucleo.compartido.concurrencia;

import java.time.Duration;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Supplier;

/**
 * Vuelve a ejecutar un caso de uso completo (recargar, aplicar la regla y guardar) cuando otra escritura se cruzó
 * con él (ADR-05). Entre intentos espera un tiempo aleatorio corto para que las escrituras cruzadas no vuelvan
 * a coincidir. Solo reintenta {@link ConflictoConcurrenciaException}; cualquier otra excepción sube de inmediato.
 *
 * <p>El caso de uso no debe tener efectos fuera de la base antes de guardar, porque puede ejecutarse varias veces.
 */
public class EjecutorConReintento {

    public static final int INTENTOS_POR_DEFECTO = 3;
    public static final Duration ESPERA_MAXIMA_POR_DEFECTO = Duration.ofMillis(50);

    private final int intentos;
    private final Duration esperaMaxima;

    public EjecutorConReintento() {
        this(INTENTOS_POR_DEFECTO, ESPERA_MAXIMA_POR_DEFECTO);
    }

    /**
     * @param intentos     intentos en total, incluido el primero
     * @param esperaMaxima espera máxima entre intentos; con {@link Duration#ZERO} no espera (para las pruebas)
     */
    public EjecutorConReintento(int intentos, Duration esperaMaxima) {
        if (intentos < 1) {
            throw new IllegalArgumentException("Se necesita al menos un intento");
        }
        this.intentos = intentos;
        this.esperaMaxima = esperaMaxima;
    }

    public <T> T ejecutar(Supplier<T> casoDeUso) {
        for (int intento = 1; ; intento++) {
            try {
                return casoDeUso.get();
            } catch (ConflictoConcurrenciaException conflicto) {
                if (intento >= intentos) {
                    throw conflicto;
                }
                esperar(conflicto);
            }
        }
    }

    public void ejecutar(Runnable casoDeUso) {
        ejecutar(() -> {
            casoDeUso.run();
            return null;
        });
    }

    private void esperar(ConflictoConcurrenciaException conflicto) {
        long maximo = esperaMaxima.toMillis();
        if (maximo <= 0) {
            return;
        }
        try {
            Thread.sleep(ThreadLocalRandom.current().nextLong(maximo + 1));
        } catch (InterruptedException interrupcion) {
            Thread.currentThread().interrupt();
            throw conflicto;
        }
    }
}
