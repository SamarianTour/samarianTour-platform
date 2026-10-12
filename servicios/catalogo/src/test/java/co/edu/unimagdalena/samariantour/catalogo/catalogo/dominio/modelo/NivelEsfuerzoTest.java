package co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

class NivelEsfuerzoTest {

    @ParameterizedTest(name = "{0} no supera {1}: {2}")
    @CsvSource({
            "BAJO,           BAJO,           true",
            "BAJO,           MODERADO,       true",
            "BAJO,           ALTA_EXIGENCIA, true",
            "MODERADO,       BAJO,           false",
            "MODERADO,       MODERADO,       true",
            "MODERADO,       ALTA_EXIGENCIA, true",
            "ALTA_EXIGENCIA, BAJO,           false",
            "ALTA_EXIGENCIA, MODERADO,       false",
            "ALTA_EXIGENCIA, ALTA_EXIGENCIA, true"})
    @DisplayName("un nivel no supera a otro si es igual o menos exigente (RF-17)")
    void noSupera(NivelEsfuerzo nivel, NivelEsfuerzo maximo, boolean esperado) {
        assertThat(nivel.noSupera(maximo)).isEqualTo(esperado);
    }

    @Test
    @DisplayName("el mayor de dos niveles es el más exigente, en cualquier orden")
    void mayor() {
        assertThat(NivelEsfuerzo.mayor(NivelEsfuerzo.BAJO, NivelEsfuerzo.MODERADO)).isEqualTo(NivelEsfuerzo.MODERADO);
        assertThat(NivelEsfuerzo.mayor(NivelEsfuerzo.ALTA_EXIGENCIA, NivelEsfuerzo.MODERADO))
                .isEqualTo(NivelEsfuerzo.ALTA_EXIGENCIA);
        assertThat(NivelEsfuerzo.mayor(NivelEsfuerzo.BAJO, NivelEsfuerzo.BAJO)).isEqualTo(NivelEsfuerzo.BAJO);
    }
}
