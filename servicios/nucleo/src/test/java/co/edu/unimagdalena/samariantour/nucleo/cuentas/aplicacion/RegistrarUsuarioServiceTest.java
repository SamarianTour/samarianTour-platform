package co.edu.unimagdalena.samariantour.nucleo.cuentas.aplicacion;

import co.edu.unimagdalena.samariantour.nucleo.cuentas.dominio.excepcion.CorreoYaRegistradoException;
import co.edu.unimagdalena.samariantour.nucleo.cuentas.dominio.excepcion.DatoInvalidoException;
import co.edu.unimagdalena.samariantour.nucleo.cuentas.dominio.excepcion.PoliticaNoVigenteException;
import co.edu.unimagdalena.samariantour.nucleo.cuentas.dominio.modelo.AceptacionPolitica;
import co.edu.unimagdalena.samariantour.nucleo.cuentas.dominio.modelo.Prestador;
import co.edu.unimagdalena.samariantour.nucleo.cuentas.dominio.modelo.Rol;
import co.edu.unimagdalena.samariantour.nucleo.cuentas.dominio.modelo.Turista;
import co.edu.unimagdalena.samariantour.nucleo.cuentas.dominio.modelo.Usuario;
import co.edu.unimagdalena.samariantour.nucleo.cuentas.dominio.puerto.entrada.comando.RegistrarUsuarioCommand;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static co.edu.unimagdalena.samariantour.nucleo.cuentas.CuentasDePrueba.AHORA;
import static co.edu.unimagdalena.samariantour.nucleo.cuentas.CuentasDePrueba.V2;
import static co.edu.unimagdalena.samariantour.nucleo.cuentas.aplicacion.EscenarioCuentas.CODIGO;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** El registro de turistas y prestadores (RF-01 a RF-03, RF-09 a RF-11). */
class RegistrarUsuarioServiceTest {

    private final EscenarioCuentas escenario = new EscenarioCuentas();

    @Test
    @DisplayName("el turista queda activo, con la política aceptada, el WhatsApp sin verificar y un código enviado")
    void turista() {
        Turista turista = (Turista) escenario.registrar.registrar(turista("Laura@Correo.co"));

        assertThat(turista.correo()).isEqualTo("laura@correo.co");
        assertThat(turista.hashContrasena()).isEqualTo("h:Playa2026");
        assertThat(turista.aceptaciones()).containsExactly(new AceptacionPolitica("v1", AHORA));
        assertThat(turista.whatsapp()).isEqualTo("+573001234567");
        assertThat(turista.puedeEnviarSolicitudes()).isFalse();
        assertThat(escenario.usuarios).containsKey(turista.id());
        assertThat(escenario.whatsappsEnviados).containsExactly("+573001234567: " + CODIGO);
        assertThat(escenario.codigos).singleElement().satisfies(codigo -> {
            assertThat(codigo.hashCodigo()).isEqualTo("h:" + CODIGO);
            assertThat(codigo.esValido(AHORA)).isTrue();
        });
    }

    @Test
    @DisplayName("el prestador se registra sin WhatsApp ni código; su RNT va en el módulo de prestadores")
    void prestador() {
        Usuario prestador = escenario.registrar.registrar(new RegistrarUsuarioCommand(Rol.PRESTADOR, "Guía Minca",
                "guia@correo.co", "Minca2026", null, true, "v1"));

        assertThat(prestador).isInstanceOf(Prestador.class);
        assertThat(escenario.whatsappsEnviados).isEmpty();
        assertThatThrownBy(() -> escenario.registrar.registrar(new RegistrarUsuarioCommand(Rol.PRESTADOR, "Otro",
                "otro@correo.co", "Minca2026", "+573001234567", true, "v1")))
                .isInstanceOf(DatoInvalidoException.class).hasMessageStartingWith("whatsapp:");
    }

    @Test
    @DisplayName("sin aceptar la política, o con una versión vieja, no se registra (RF-09, RF-10)")
    void politica() {
        assertThatThrownBy(() -> escenario.registrar.registrar(new RegistrarUsuarioCommand(Rol.TURISTA, "Laura",
                "laura@correo.co", "Playa2026", "+573001234567", false, "v1")))
                .isInstanceOf(DatoInvalidoException.class).hasMessageStartingWith("aceptaPolitica:");

        escenario.vigente = V2;
        assertThatThrownBy(() -> escenario.registrar.registrar(turista("laura@correo.co")))
                .isInstanceOf(PoliticaNoVigenteException.class);
        assertThat(escenario.usuarios).isEmpty();
    }

    @Test
    @DisplayName("el correo repetido con otras mayúsculas responde ya registrado; una contraseña débil, dato inválido")
    void correoYContrasena() {
        escenario.registrar.registrar(turista("laura@correo.co"));

        assertThatThrownBy(() -> escenario.registrar.registrar(turista("LAURA@correo.co")))
                .isInstanceOf(CorreoYaRegistradoException.class);
        assertThatThrownBy(() -> escenario.registrar.registrar(new RegistrarUsuarioCommand(Rol.TURISTA, "Ana",
                "ana@correo.co", "corta1", "+573001234567", true, "v1")))
                .isInstanceOf(DatoInvalidoException.class).hasMessageStartingWith("contrasena:");
    }

    @Test
    @DisplayName("las cuentas internas no se registran solas (RF-01, RF-81)")
    void soloTuristaYPrestador() {
        assertThatThrownBy(() -> escenario.registrar.registrar(new RegistrarUsuarioCommand(Rol.ADMINISTRADOR,
                "Admin", "admin@correo.co", "Admin2026", null, true, "v1")))
                .isInstanceOf(DatoInvalidoException.class).hasMessageStartingWith("rol:");
    }

    private static RegistrarUsuarioCommand turista(String correo) {
        return new RegistrarUsuarioCommand(Rol.TURISTA, "Laura Gómez", correo, "Playa2026", "+57 300 123 4567", true,
                "v1");
    }
}
