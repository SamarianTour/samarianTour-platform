package co.edu.unimagdalena.samariantour.nucleo.cuentas.dominio.modelo;

import co.edu.unimagdalena.samariantour.nucleo.cuentas.dominio.politica.PoliticaBloqueo;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.UUID;

import static co.edu.unimagdalena.samariantour.nucleo.cuentas.CuentasDePrueba.AHORA;
import static co.edu.unimagdalena.samariantour.nucleo.cuentas.CuentasDePrueba.HASHER;
import static co.edu.unimagdalena.samariantour.nucleo.cuentas.CuentasDePrueba.V1;
import static co.edu.unimagdalena.samariantour.nucleo.cuentas.CuentasDePrueba.V2;
import static co.edu.unimagdalena.samariantour.nucleo.cuentas.CuentasDePrueba.cuenta;
import static co.edu.unimagdalena.samariantour.nucleo.cuentas.CuentasDePrueba.turista;
import static org.assertj.core.api.Assertions.assertThat;

/** La cuenta de cualquier rol (RF-04, RF-05, RF-09, RF-10). */
class UsuarioTest {

    @Test
    @DisplayName("se reconstruye con la subclase de su rol, como en el diccionario de clases")
    void subclasePorRol() {
        for (Rol rol : Rol.values()) {
            assertThat(cuenta(rol, "x@correo.co").rol()).isEqualTo(rol);
        }
        assertThat(cuenta(Rol.PRESTADOR, "x@correo.co")).isInstanceOf(Prestador.class);
        assertThat(cuenta(Rol.GESTOR_DESTINO, "x@correo.co")).isInstanceOf(GestorDestino.class);
    }

    @Test
    @DisplayName("el correo se guarda en minúsculas y sin espacios: es el mismo correo con otras mayúsculas")
    void correoNormalizado() {
        assertThat(cuenta(Rol.TURISTA, "  Laura.Gomez@Correo.CO ").correo()).isEqualTo("laura.gomez@correo.co");
    }

    @Test
    @DisplayName("autentica con la contraseña correcta; una cuenta sin contraseña nunca coincide")
    void autenticar() {
        Usuario usuario = cuenta(Rol.TURISTA, "x@correo.co");

        assertThat(usuario.autenticar("Clave1234", HASHER)).isTrue();
        assertThat(usuario.autenticar("otra", HASHER)).isFalse();
        assertThat(usuario.autenticar(null, HASHER)).isFalse();
        Usuario sinContrasena = Usuario.reconstruir(Rol.OPERADOR_DISPONIBILIDAD,
                DatosCuenta.nueva(UUID.randomUUID(), "Operador", "op@correo.co", null, AHORA), null, false);
        assertThat(sinContrasena.autenticar("", HASHER)).isFalse();
    }

    @Nested
    @DisplayName("Bloqueo (RF-05)")
    class Bloqueo {

        @Test
        @DisplayName("el quinto fallo consecutivo bloquea 15 minutos y la cuenta vuelve a contar desde cero")
        void quintoFallo() {
            Usuario usuario = cuenta(Rol.TURISTA, "x@correo.co");
            for (int i = 1; i < PoliticaBloqueo.INTENTOS_MAXIMOS; i++) {
                usuario.registrarIntentoFallido(AHORA);
            }
            assertThat(usuario.estaBloqueada(AHORA)).isFalse();
            assertThat(usuario.intentosFallidos()).isEqualTo(4);

            usuario.registrarIntentoFallido(AHORA);

            assertThat(usuario.estaBloqueada(AHORA)).isTrue();
            assertThat(usuario.estaBloqueada(AHORA.plus(Duration.ofMinutes(15)).minusMillis(1))).isTrue();
            assertThat(usuario.estaBloqueada(AHORA.plus(Duration.ofMinutes(15)))).isFalse();
            assertThat(usuario.intentosFallidos()).isZero();
        }

        @Test
        @DisplayName("un ingreso correcto corta la racha: los fallos tienen que ser consecutivos")
        void ingresoCortaLaRacha() {
            Usuario usuario = cuenta(Rol.TURISTA, "x@correo.co");
            for (int i = 0; i < 4; i++) {
                usuario.registrarIntentoFallido(AHORA);
            }
            usuario.registrarIngreso();
            usuario.registrarIntentoFallido(AHORA);

            assertThat(usuario.estaBloqueada(AHORA)).isFalse();
            assertThat(usuario.intentosFallidos()).isEqualTo(1);
        }
    }

    @Test
    @DisplayName("deja constancia de la versión aceptada con su fecha; una versión nueva hay que aceptarla de nuevo")
    void politica() {
        Usuario usuario = cuenta(Rol.PRESTADOR, "x@correo.co");
        assertThat(usuario.debeAceptarPolitica(V1)).isTrue();

        usuario.aceptarPolitica(V1, AHORA);
        usuario.aceptarPolitica(V1, AHORA.plusSeconds(60));

        assertThat(usuario.aceptaciones()).containsExactly(new AceptacionPolitica("v1", AHORA));
        assertThat(usuario.debeAceptarPolitica(V1)).isFalse();
        assertThat(usuario.debeAceptarPolitica(V2)).isTrue();
    }

    @Test
    @DisplayName("el turista solo envía solicitudes con el WhatsApp verificado (RF-30)")
    void turistaVerificado() {
        assertThat(turista("t@correo.co", "+573001234567", true).puedeEnviarSolicitudes()).isTrue();
        assertThat(turista("t@correo.co", "+573001234567", false).puedeEnviarSolicitudes()).isFalse();
        assertThat(turista("t@correo.co", null, true).whatsappVerificado()).isFalse();
    }
}
