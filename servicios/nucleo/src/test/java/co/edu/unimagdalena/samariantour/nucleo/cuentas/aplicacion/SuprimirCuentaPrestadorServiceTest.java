package co.edu.unimagdalena.samariantour.nucleo.cuentas.aplicacion;

import co.edu.unimagdalena.samariantour.nucleo.cuentas.dominio.excepcion.ContrasenaIncorrectaException;
import co.edu.unimagdalena.samariantour.nucleo.cuentas.dominio.excepcion.CuentaBloqueadaException;
import co.edu.unimagdalena.samariantour.nucleo.cuentas.dominio.excepcion.CuentaNoEncontradaException;
import co.edu.unimagdalena.samariantour.nucleo.cuentas.dominio.modelo.Rol;
import co.edu.unimagdalena.samariantour.nucleo.cuentas.dominio.modelo.Usuario;
import co.edu.unimagdalena.samariantour.nucleo.cuentas.dominio.puerto.entrada.comando.RegistrarUsuarioCommand;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** La cuenta en la supresión del prestador (RF-05, RF-13, DN-45). */
class SuprimirCuentaPrestadorServiceTest {

    private final EscenarioCuentas escenario = new EscenarioCuentas();
    private Usuario guia;

    @BeforeEach
    void registrar() {
        guia = escenario.registrar.registrar(new RegistrarUsuarioCommand(Rol.PRESTADOR, "Juan Pérez",
                "juan@correo.co", "Minca2026", null, true, "v1"));
    }

    @Test
    @DisplayName("con la contraseña correcta se anonimiza y ya no se puede volver a suprimir")
    void suprime() {
        escenario.supresionPrestador.verificarContrasena(guia.id(), "Minca2026");
        escenario.supresionPrestador.suprimir(guia.id());

        Usuario suprimido = escenario.usuarios.get(guia.id());
        assertThat(suprimido.nombre()).isEqualTo(Usuario.NOMBRE_SUPRIMIDO);
        assertThat(suprimido.correo()).isNotEqualTo("juan@correo.co");
        assertThatThrownBy(() -> escenario.supresionPrestador.verificarContrasena(guia.id(), "Minca2026"))
                .isInstanceOf(CuentaNoEncontradaException.class);
    }

    @Test
    @DisplayName("la contraseña equivocada cuenta para el bloqueo: al quinto fallo queda bloqueada (RF-05)")
    void bloqueo() {
        for (int intento = 1; intento < 5; intento++) {
            assertThatThrownBy(() -> escenario.supresionPrestador.verificarContrasena(guia.id(), "otra"))
                    .isInstanceOf(ContrasenaIncorrectaException.class);
        }
        assertThatThrownBy(() -> escenario.supresionPrestador.verificarContrasena(guia.id(), "otra"))
                .isInstanceOf(CuentaBloqueadaException.class);
        assertThatThrownBy(() -> escenario.supresionPrestador.verificarContrasena(guia.id(), "Minca2026"))
                .isInstanceOf(CuentaBloqueadaException.class);
    }

    @Test
    @DisplayName("un turista no es un prestador")
    void turista() {
        Usuario laura = escenario.registrar.registrar(new RegistrarUsuarioCommand(Rol.TURISTA, "Laura Gómez",
                "laura@correo.co", "Playa2026", "+573001234567", true, "v1"));

        assertThatThrownBy(() -> escenario.supresionPrestador.verificarContrasena(laura.id(), "Playa2026"))
                .isInstanceOf(CuentaNoEncontradaException.class);
    }
}
