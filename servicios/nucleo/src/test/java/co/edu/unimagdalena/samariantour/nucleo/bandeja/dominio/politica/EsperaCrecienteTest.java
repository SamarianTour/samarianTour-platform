package co.edu.unimagdalena.samariantour.nucleo.bandeja.dominio.politica;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EsperaCrecienteTest {

    @Test
    @DisplayName("5 s tras el primer fallo, el doble tras cada uno de los siguientes y nunca más de 5 minutos")
    void creceHastaElMaximo() {
        assertThat(EsperaCreciente.despuesDe(1)).isEqualTo(Duration.ofSeconds(5));
        assertThat(EsperaCreciente.despuesDe(2)).isEqualTo(Duration.ofSeconds(10));
        assertThat(EsperaCreciente.despuesDe(3)).isEqualTo(Duration.ofSeconds(20));
        assertThat(EsperaCreciente.despuesDe(6)).isEqualTo(Duration.ofSeconds(160));
        assertThat(EsperaCreciente.despuesDe(7)).isEqualTo(Duration.ofMinutes(5));
        assertThat(EsperaCreciente.despuesDe(1_000)).isEqualTo(Duration.ofMinutes(5));
    }

    @Test
    @DisplayName("solo se calcula después de un fallo")
    void sinFallos() {
        assertThatThrownBy(() -> EsperaCreciente.despuesDe(0)).isInstanceOf(IllegalArgumentException.class);
    }
}
