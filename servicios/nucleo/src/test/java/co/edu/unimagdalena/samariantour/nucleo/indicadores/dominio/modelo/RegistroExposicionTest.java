package co.edu.unimagdalena.samariantour.nucleo.indicadores.dominio.modelo;

import co.edu.unimagdalena.samariantour.nucleo.indicadores.dominio.excepcion.DatoInvalidoException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** El registro de exposición que envía la recomendación (RET-02, DN-42). */
class RegistroExposicionTest {

    private static final Instant AHORA = Instant.parse("2026-11-02T15:00:00Z");
    private static final OfertaExpuesta CAMINATA = new OfertaExpuesta("b1", UUID.randomUUID(), 1);

    @Test
    @DisplayName("lleva la consulta, el origen, el motor y de 1 a 10 ofertas")
    void valido() {
        RegistroExposicion registro = new RegistroExposicion(UUID.randomUUID(), AHORA,
                OrigenExposicion.RECOMENDACION, "IA", List.of(CAMINATA));

        assertThat(registro.ofertas()).containsExactly(CAMINATA);
    }

    @Test
    @DisplayName("sin fecha, con otro motor, sin ofertas o con más de 10 es un dato inválido")
    void invalido() {
        assertThatThrownBy(() -> new RegistroExposicion(UUID.randomUUID(), null, OrigenExposicion.RECOMENDACION,
                "REGLAS", List.of(CAMINATA))).isInstanceOf(DatoInvalidoException.class).hasMessageStartingWith("fecha");
        assertThatThrownBy(() -> new RegistroExposicion(UUID.randomUUID(), AHORA, OrigenExposicion.RECOMENDACION,
                "GPT", List.of(CAMINATA))).hasMessageStartingWith("motor");
        assertThatThrownBy(() -> new RegistroExposicion(UUID.randomUUID(), AHORA, OrigenExposicion.ALTERNATIVAS,
                "REGLAS", List.of())).hasMessageStartingWith("ofertas");
        assertThatThrownBy(() -> new RegistroExposicion(UUID.randomUUID(), AHORA, OrigenExposicion.ALTERNATIVAS,
                "REGLAS", Collections.nCopies(11, CAMINATA))).hasMessageStartingWith("ofertas");
    }

    @Test
    @DisplayName("cada oferta trae su id, su prestador y una posición desde 1")
    void oferta() {
        assertThatThrownBy(() -> new OfertaExpuesta(" ", UUID.randomUUID(), 1)).hasMessageStartingWith("ofertaId");
        assertThatThrownBy(() -> new OfertaExpuesta("b1", null, 1)).hasMessageStartingWith("prestadorId");
        assertThatThrownBy(() -> new OfertaExpuesta("b1", UUID.randomUUID(), 0)).hasMessageStartingWith("posicion");
    }
}
