package co.edu.unimagdalena.samariantour.nucleo.compartido.cifrado;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** El cifrado en reposo de los datos personales (RNF-08, DN-18). */
class CifradorDatosTest {

    private static final String CLAVE = Base64.getEncoder()
            .encodeToString("0123456789abcdef0123456789abcdef".getBytes());
    private static final String OTRA = Base64.getEncoder()
            .encodeToString("fedcba9876543210fedcba9876543210".getBytes());

    private final CifradorDatos cifrador = CifradorDatos.conClave(CLAVE);

    @Test
    @DisplayName("descifra lo que cifró; el mismo texto da un cifrado distinto cada vez y no se lee en él")
    void idaYVuelta() {
        String primero = cifrador.cifrar("Laura Gómez");
        String segundo = cifrador.cifrar("Laura Gómez");

        assertThat(primero).startsWith("v1:").doesNotContain("Laura").isNotEqualTo(segundo);
        assertThat(cifrador.descifrar(primero)).isEqualTo("Laura Gómez");
        assertThat(cifrador.cifrar(null)).isNull();
        assertThat(cifrador.descifrar(null)).isNull();
    }

    @Test
    @DisplayName("otra clave o un valor alterado no se descifran")
    void otraClaveOAlterado() {
        String cifrado = cifrador.cifrar("+573001234567");
        // Cambia un carácter del medio, que siempre es un byte del vector o del cifrado.
        String alterado = cifrado.substring(0, 20) + (cifrado.charAt(20) == 'A' ? 'B' : 'A') + cifrado.substring(21);

        assertThatThrownBy(() -> CifradorDatos.conClave(OTRA).descifrar(cifrado))
                .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> cifrador.descifrar(alterado)).isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("sin clave o con una de otro tamaño no arranca")
    void claveObligatoria() {
        assertThatThrownBy(() -> CifradorDatos.conClave("")).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("NUCLEO_CIFRADO_CLAVE");
        assertThatThrownBy(() -> CifradorDatos.conClave(Base64.getEncoder().encodeToString(new byte[16])))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
