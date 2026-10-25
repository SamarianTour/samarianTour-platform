package co.edu.unimagdalena.samariantour.recomendacion.recomendacion.dominio.modelo;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class AlertaClimaticaTest {

    private static final Instant AHORA = Instant.parse("2026-10-24T15:00:00Z");

    @Test
    @DisplayName("activa sin fin anunciado o con el fin después de ahora; vencida si el fin ya pasó o es ahora mismo")
    void vigencia() {
        assertThat(alerta("sin-fin", NivelAlerta.AMARILLA, AHORA.minus(Duration.ofDays(3)), null).activa(AHORA))
                .isTrue();
        assertThat(alerta("vigente", NivelAlerta.AMARILLA, AHORA, AHORA.plusSeconds(1)).activa(AHORA)).isTrue();
        assertThat(alerta("vencida", NivelAlerta.AMARILLA, AHORA, AHORA.minusSeconds(1)).activa(AHORA)).isFalse();
        assertThat(alerta("justo", NivelAlerta.AMARILLA, AHORA, AHORA).activa(AHORA)).isFalse();
    }

    @Test
    @DisplayName("para mostrar: de mayor a menor nivel; con el mismo nivel, la más reciente primero y sin fecha al final")
    void orden() {
        List<AlertaClimatica> ordenadas = Stream.of(
                        alerta("amarilla-sin-fecha", NivelAlerta.AMARILLA, null, null),
                        alerta("amarilla-vieja", NivelAlerta.AMARILLA, AHORA.minus(Duration.ofHours(5)), null),
                        alerta("roja-sin-fecha", NivelAlerta.ROJA, null, null),
                        alerta("roja", NivelAlerta.ROJA, AHORA.minus(Duration.ofDays(1)), null),
                        alerta("amarilla-nueva", NivelAlerta.AMARILLA, AHORA.minus(Duration.ofHours(1)), null),
                        alerta("naranja", NivelAlerta.NARANJA, AHORA, null))
                .sorted(AlertaClimatica.ORDEN_PARA_MOSTRAR).toList();

        assertThat(ordenadas).extracting(AlertaClimatica::id).containsExactly(
                "roja", "roja-sin-fecha", "naranja", "amarilla-nueva", "amarilla-vieja", "amarilla-sin-fecha");
    }

    private static AlertaClimatica alerta(String id, NivelAlerta nivel, Instant emitida, Instant hasta) {
        return new AlertaClimatica(id, "a00000000000000000000005", TipoAlerta.LLUVIAS, nivel, "Lluvias", null,
                "IDEAM (simulado)", emitida, hasta);
    }
}
