package co.edu.unimagdalena.samariantour.nucleo.bandeja.infraestructura.entrada.programada;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayDeque;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/** La tarea que publica la bandeja de salida (DN-06). */
class PublicacionProgramadaTest {

    @Test
    @DisplayName("publica lote tras lote hasta que no queda ninguno por enviar")
    void hastaVaciar() {
        Queue<Integer> lotes = new ArrayDeque<>(List.of(10, 10, 3, 0, 10));
        AtomicInteger llamadas = new AtomicInteger();

        new PublicacionProgramada(() -> {
            llamadas.incrementAndGet();
            return lotes.remove();
        }).publicar();

        assertThat(llamadas).hasValue(4);
    }

    @Test
    @DisplayName("se detiene en el tope de lotes por ejecución; lo que falte sale en la siguiente")
    void tope() {
        AtomicInteger llamadas = new AtomicInteger();

        new PublicacionProgramada(() -> {
            llamadas.incrementAndGet();
            return 10;
        }).publicar();

        assertThat(llamadas).hasValue(PublicacionProgramada.LOTES_POR_EJECUCION);
    }
}
