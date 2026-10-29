package co.edu.unimagdalena.samariantour.nucleo.cuentas.dominio.modelo;

import co.edu.unimagdalena.samariantour.nucleo.cuentas.dominio.excepcion.DatoInvalidoException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThatNoException;

/** Los datos de una cuenta (RF-02, RF-03, DN-28). */
class ValidacionesTest {

    @ParameterizedTest
    @ValueSource(strings = {"Clave123", "contraseña9", "12345678a", "Ñandú2026"})
    @DisplayName("una contraseña de 8 o más caracteres con una letra y un número es válida")
    void contrasenaValida(String contrasena) {
        assertThatNoException().isThrownBy(() -> Validaciones.contrasena(contrasena));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "Clave12", "solotexto", "12345678"})
    @DisplayName("una contraseña corta, sin letras o sin números responde DATO_INVALIDO en el campo contrasena")
    void contrasenaInvalida(String contrasena) {
        assertThatThrownBy(() -> Validaciones.contrasena(contrasena))
                .isInstanceOf(DatoInvalidoException.class).hasMessageStartingWith("contrasena:");
    }

    @Test
    @DisplayName("una contraseña de más de 72 caracteres no se acepta: BCrypt ignoraría el resto")
    void contrasenaLarga() {
        assertThatThrownBy(() -> Validaciones.contrasena("a1" + "x".repeat(71)))
                .isInstanceOf(DatoInvalidoException.class);
    }

    @Test
    @DisplayName("el WhatsApp va con indicativo; se quitan espacios y guiones")
    void whatsapp() {
        assertThat(Validaciones.whatsapp("+57 300 123-4567")).isEqualTo("+573001234567");
        assertThatThrownBy(() -> Validaciones.whatsapp("3001234567")).isInstanceOf(DatoInvalidoException.class)
                .hasMessageStartingWith("whatsapp:");
        assertThatThrownBy(() -> Validaciones.whatsapp(null)).isInstanceOf(DatoInvalidoException.class);
    }

    @Test
    @DisplayName("el correo debe tener forma de correo y el nombre no puede quedar vacío")
    void correoYNombre() {
        assertThatThrownBy(() -> Validaciones.correo("laura.correo.co")).isInstanceOf(DatoInvalidoException.class)
                .hasMessageStartingWith("correo:");
        assertThatThrownBy(() -> Validaciones.nombre("   ")).isInstanceOf(DatoInvalidoException.class)
                .hasMessageStartingWith("nombre:");
        assertThat(Validaciones.nombre("  Laura  ")).isEqualTo("Laura");
    }
}
