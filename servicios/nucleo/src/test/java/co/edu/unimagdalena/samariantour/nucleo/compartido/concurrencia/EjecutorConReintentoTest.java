package co.edu.unimagdalena.samariantour.nucleo.compartido.concurrencia;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EjecutorConReintentoTest {

    private final EjecutorConReintento ejecutor = new EjecutorConReintento(3, Duration.ZERO);
    private final AtomicInteger intentos = new AtomicInteger();

    @Test
    @DisplayName("sin conflicto ejecuta el caso de uso una sola vez")
    void sinConflicto() {
        String resultado = ejecutor.ejecutar(() -> {
            intentos.incrementAndGet();
            return "hecho";
        });

        assertThat(resultado).isEqualTo("hecho");
        assertThat(intentos).hasValue(1);
    }

    @Test
    @DisplayName("ante un conflicto vuelve a ejecutar el caso de uso completo")
    void reintentaTrasConflicto() {
        String resultado = ejecutor.ejecutar(() -> {
            if (intentos.incrementAndGet() < 3) {
                throw new ConflictoConcurrenciaException("cruce");
            }
            return "hecho";
        });

        assertThat(resultado).isEqualTo("hecho");
        assertThat(intentos).hasValue(3);
    }

    @Test
    @DisplayName("si el conflicto sigue después de 3 intentos, lo deja subir")
    void agotaLosIntentos() {
        assertThatThrownBy(() -> ejecutor.ejecutar(() -> {
            intentos.incrementAndGet();
            throw new ConflictoConcurrenciaException("cruce");
        })).isInstanceOf(ConflictoConcurrenciaException.class);
        assertThat(intentos).hasValue(3);
    }

    @Test
    @DisplayName("cualquier otra excepción sube de inmediato, sin reintentar")
    void noReintentaOtrasExcepciones() {
        assertThatThrownBy(() -> ejecutor.ejecutar(() -> {
            intentos.incrementAndGet();
            throw new IllegalStateException("regla incumplida");
        })).isInstanceOf(IllegalStateException.class);
        assertThat(intentos).hasValue(1);
    }

    @Test
    @DisplayName("con espera entre intentos, el reintento también funciona")
    void conEspera() {
        EjecutorConReintento conEspera = new EjecutorConReintento(2, Duration.ofMillis(5));

        conEspera.ejecutar(() -> {
            if (intentos.incrementAndGet() == 1) {
                throw new ConflictoConcurrenciaException("cruce");
            }
        });

        assertThat(intentos).hasValue(2);
    }
}
