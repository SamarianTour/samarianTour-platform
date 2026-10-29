package co.edu.unimagdalena.samariantour.nucleo.cuentas.aplicacion;

import co.edu.unimagdalena.samariantour.nucleo.cuentas.dominio.excepcion.CodigoNoVigenteException;
import co.edu.unimagdalena.samariantour.nucleo.cuentas.dominio.excepcion.CuentaBloqueadaException;
import co.edu.unimagdalena.samariantour.nucleo.cuentas.dominio.excepcion.DatoInvalidoException;
import co.edu.unimagdalena.samariantour.nucleo.cuentas.dominio.modelo.Rol;
import co.edu.unimagdalena.samariantour.nucleo.cuentas.dominio.modelo.SesionIniciada;
import co.edu.unimagdalena.samariantour.nucleo.cuentas.dominio.modelo.Usuario;
import co.edu.unimagdalena.samariantour.nucleo.cuentas.dominio.puerto.entrada.comando.RegistrarUsuarioCommand;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static co.edu.unimagdalena.samariantour.nucleo.cuentas.CuentasDePrueba.AHORA;
import static co.edu.unimagdalena.samariantour.nucleo.cuentas.CuentasDePrueba.HASHER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Restablecer la contraseña con el enlace del correo (RF-07, RF-08, DN-30). */
class RestablecimientoServiceTest {

    private final EscenarioCuentas escenario = new EscenarioCuentas();
    private Usuario laura;

    @BeforeEach
    void registrar() {
        laura = escenario.registrar.registrar(new RegistrarUsuarioCommand(Rol.TURISTA, "Laura Gómez",
                "laura@correo.co", "Playa2026", "+573001234567", true, "v1"));
    }

    @Test
    @DisplayName("el enlace cambia la contraseña, quita el bloqueo y cierra las sesiones; no sirve otra vez")
    void restablecer() {
        SesionIniciada abierta = escenario.iniciarSesion.iniciar("laura@correo.co", "Playa2026");
        for (int i = 0; i < 5; i++) {
            try {
                escenario.iniciarSesion.iniciar("laura@correo.co", "otra");
            } catch (RuntimeException esperada) {
                // Cinco fallos: la cuenta queda bloqueada.
            }
        }
        assertThatThrownBy(() -> escenario.iniciarSesion.iniciar("laura@correo.co", "Playa2026"))
                .isInstanceOf(CuentaBloqueadaException.class);

        escenario.solicitarRestablecimiento.solicitar(" LAURA@correo.co ");
        assertThat(escenario.correosEnviados).singleElement().asString().startsWith("laura@correo.co ");
        escenario.restablecerContrasena.restablecer(escenario.ultimoEnlace(), "Nueva2026");

        assertThat(escenario.iniciarSesion.iniciar("laura@correo.co", "Nueva2026").usuario().id())
                .isEqualTo(laura.id());
        assertThat(escenario.sesiones.get(abierta.renovacion().id()).revocadaEn()).isEqualTo(AHORA);
        assertThatThrownBy(() -> escenario.restablecerContrasena.restablecer(escenario.ultimoEnlace(), "Otra2026"))
                .isInstanceOf(CodigoNoVigenteException.class);
    }

    @Test
    @DisplayName("un correo que no está registrado no recibe nada y la respuesta es la misma")
    void correoDesconocido() {
        escenario.solicitarRestablecimiento.solicitar("nadie@correo.co");

        assertThat(escenario.correosEnviados).isEmpty();
        assertThat(escenario.codigos).hasSize(1);
    }

    @Test
    @DisplayName("el enlace vence a los 30 minutos y uno nuevo invalida el anterior (RF-08)")
    void vencidoOReemplazado() {
        escenario.solicitarRestablecimiento.solicitar("laura@correo.co");
        String primero = escenario.ultimoEnlace();
        escenario.ahora = AHORA.plus(Duration.ofMinutes(1));
        escenario.solicitarRestablecimiento.solicitar("laura@correo.co");

        assertThatThrownBy(() -> escenario.restablecerContrasena.restablecer(primero, "Nueva2026"))
                .isInstanceOf(CodigoNoVigenteException.class);

        escenario.ahora = AHORA.plus(Duration.ofMinutes(31));
        assertThatThrownBy(() -> escenario.restablecerContrasena.restablecer(escenario.ultimoEnlace(), "Nueva2026"))
                .isInstanceOf(CodigoNoVigenteException.class);
    }

    @Test
    @DisplayName("una contraseña débil no gasta el enlace; un token alterado no sirve")
    void contrasenaDebilOTokenAlterado() {
        escenario.solicitarRestablecimiento.solicitar("laura@correo.co");
        String enlace = escenario.ultimoEnlace();

        assertThatThrownBy(() -> escenario.restablecerContrasena.restablecer(enlace, "corta"))
                .isInstanceOf(DatoInvalidoException.class);
        assertThatThrownBy(() -> escenario.restablecerContrasena.restablecer(enlace + "x", "Nueva2026"))
                .isInstanceOf(CodigoNoVigenteException.class);
        assertThatThrownBy(() -> escenario.restablecerContrasena.restablecer("cualquier-cosa", "Nueva2026"))
                .isInstanceOf(CodigoNoVigenteException.class);

        escenario.restablecerContrasena.restablecer(enlace, "Nueva2026");
        assertThat(laura.autenticar("Nueva2026", HASHER)).isTrue();
    }
}
