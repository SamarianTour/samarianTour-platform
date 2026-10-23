package co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo;

import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.excepcion.DatoInvalidoException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FotoTest {

    @Test
    @DisplayName("una foto nueva tiene un identificador aleatorio que no se repite")
    void nueva() {
        assertThat(Foto.nueva()).isNotEqualTo(Foto.nueva());
        assertThat(new Foto("3F2504E0-4F89-11D3-9A0C-0305E82C3301").id())
                .isEqualTo("3f2504e0-4f89-11d3-9a0c-0305e82c3301");
    }

    @ParameterizedTest(name = "\"{0}\" no es un identificador de foto")
    @ValueSource(strings = {"foto.jpg", "../../etc/passwd", "https://ejemplo.co/foto.jpg", " "})
    @DisplayName("el identificador es un UUID: no puede ser una ruta ni una URL")
    void identificadorInvalido(String id) {
        assertThatThrownBy(() -> new Foto(id)).isInstanceOf(DatoInvalidoException.class);
    }
}
