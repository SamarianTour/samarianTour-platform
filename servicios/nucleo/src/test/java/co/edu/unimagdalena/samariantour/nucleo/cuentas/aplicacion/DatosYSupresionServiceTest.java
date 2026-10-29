package co.edu.unimagdalena.samariantour.nucleo.cuentas.aplicacion;

import co.edu.unimagdalena.samariantour.nucleo.cuentas.dominio.excepcion.ContrasenaIncorrectaException;
import co.edu.unimagdalena.samariantour.nucleo.cuentas.dominio.excepcion.CredencialesInvalidasException;
import co.edu.unimagdalena.samariantour.nucleo.cuentas.dominio.excepcion.CuentaBloqueadaException;
import co.edu.unimagdalena.samariantour.nucleo.cuentas.dominio.excepcion.CuentaNoEncontradaException;
import co.edu.unimagdalena.samariantour.nucleo.cuentas.dominio.excepcion.DatoInvalidoException;
import co.edu.unimagdalena.samariantour.nucleo.cuentas.dominio.modelo.AceptacionPolitica;
import co.edu.unimagdalena.samariantour.nucleo.cuentas.dominio.modelo.EstadoCuenta;
import co.edu.unimagdalena.samariantour.nucleo.cuentas.dominio.modelo.EstadoSupresion;
import co.edu.unimagdalena.samariantour.nucleo.cuentas.dominio.modelo.Rol;
import co.edu.unimagdalena.samariantour.nucleo.cuentas.dominio.modelo.SesionIniciada;
import co.edu.unimagdalena.samariantour.nucleo.cuentas.dominio.modelo.Turista;
import co.edu.unimagdalena.samariantour.nucleo.cuentas.dominio.modelo.Usuario;
import co.edu.unimagdalena.samariantour.nucleo.cuentas.dominio.puerto.entrada.comando.ActualizarDatosCommand;
import co.edu.unimagdalena.samariantour.nucleo.cuentas.dominio.puerto.entrada.comando.RegistrarUsuarioCommand;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static co.edu.unimagdalena.samariantour.nucleo.cuentas.CuentasDePrueba.AHORA;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Los datos personales (RF-12) y su supresión inmediata (RF-13, DN-31). */
class DatosYSupresionServiceTest {

    private final EscenarioCuentas escenario = new EscenarioCuentas();
    private Turista laura;

    @BeforeEach
    void registrar() {
        laura = (Turista) escenario.registrar.registrar(new RegistrarUsuarioCommand(Rol.TURISTA, "Laura Gómez",
                "laura@correo.co", "Playa2026", "+573001234567", true, "v1"));
        escenario.verificarWhatsApp.verificar(laura.id(), EscenarioCuentas.CODIGO);
    }

    @Nested
    @DisplayName("Datos personales (RF-12)")
    class Datos {

        @Test
        @DisplayName("cambiar el nombre no toca el WhatsApp; cambiar el WhatsApp lo deja sin verificar y envía código")
        void actualizar() {
            escenario.datosPersonales.actualizar(new ActualizarDatosCommand(laura.id(), " Laura Gómez Ruiz ",
                    "+573001234567"));
            assertThat(laura.nombre()).isEqualTo("Laura Gómez Ruiz");
            assertThat(laura.puedeEnviarSolicitudes()).isTrue();
            assertThat(escenario.whatsappsEnviados).hasSize(1);

            escenario.datosPersonales.actualizar(new ActualizarDatosCommand(laura.id(), "Laura Gómez Ruiz",
                    "+573009998877"));

            assertThat(laura.whatsapp()).isEqualTo("+573009998877");
            assertThat(laura.puedeEnviarSolicitudes()).isFalse();
            assertThat(escenario.whatsappsEnviados).last().asString().startsWith("+573009998877");
        }

        @Test
        @DisplayName("el prestador cambia su nombre pero no manda WhatsApp; un nombre vacío no se acepta")
        void prestador() {
            Usuario guia = escenario.registrar.registrar(new RegistrarUsuarioCommand(Rol.PRESTADOR, "Guía",
                    "guia@correo.co", "Minca2026", null, true, "v1"));

            assertThat(escenario.datosPersonales.actualizar(new ActualizarDatosCommand(guia.id(), "Guía de Minca",
                    null)).nombre()).isEqualTo("Guía de Minca");
            assertThatThrownBy(() -> escenario.datosPersonales.actualizar(new ActualizarDatosCommand(guia.id(),
                    "Guía", "+573001112233"))).isInstanceOf(DatoInvalidoException.class)
                    .hasMessageStartingWith("whatsapp:");
            assertThatThrownBy(() -> escenario.datosPersonales.actualizar(new ActualizarDatosCommand(guia.id(), " ",
                    null))).isInstanceOf(DatoInvalidoException.class).hasMessageStartingWith("nombre:");
        }
    }

    @Nested
    @DisplayName("Supresión (RF-13)")
    class Supresion {

        @Test
        @DisplayName("anonimiza al instante, deja la constancia, cierra las sesiones y avisa al catálogo")
        void suprimir() {
            SesionIniciada sesion = escenario.iniciarSesion.iniciar("laura@correo.co", "Playa2026");

            escenario.supresion.suprimir(laura.id(), "Playa2026");

            assertThat(laura.estado()).isEqualTo(EstadoCuenta.SUPRIMIDA);
            assertThat(laura.nombre()).isEqualTo(Usuario.NOMBRE_SUPRIMIDO);
            assertThat(laura.correo()).isEqualTo("suprimido-" + laura.id() + "@cuentas.invalid");
            assertThat(laura.hashContrasena()).isNull();
            assertThat(laura.whatsapp()).isNull();
            assertThat(laura.supresion()).hasValueSatisfying(constancia -> {
                assertThat(constancia.estado()).isEqualTo(EstadoSupresion.EJECUTADA);
                assertThat(constancia.fechaSolicitud()).isEqualTo(AHORA).isEqualTo(constancia.fechaEjecucion());
            });
            assertThat(laura.aceptaciones()).containsExactly(new AceptacionPolitica("v1", AHORA));
            assertThat(escenario.sesiones.get(sesion.renovacion().id()).revocadaEn()).isEqualTo(AHORA);
            assertThat(escenario.supresionesAvisadas).containsExactly(laura.id());

            assertThatThrownBy(() -> escenario.iniciarSesion.iniciar("laura@correo.co", "Playa2026"))
                    .isInstanceOf(CredencialesInvalidasException.class);
            assertThatThrownBy(() -> escenario.datosPersonales.consultar(laura.id()))
                    .isInstanceOf(CuentaNoEncontradaException.class);
            assertThat(escenario.registrar.registrar(new RegistrarUsuarioCommand(Rol.TURISTA, "Laura",
                    "laura@correo.co", "Playa2026", "+573001234567", true, "v1")).id()).isNotEqualTo(laura.id());
        }

        @Test
        @DisplayName("sin la contraseña correcta no suprime y el fallo cuenta: al quinto, la cuenta se bloquea")
        void contrasenaIncorrecta() {
            for (int i = 0; i < 4; i++) {
                assertThatThrownBy(() -> escenario.supresion.suprimir(laura.id(), "otra"))
                        .isInstanceOf(ContrasenaIncorrectaException.class);
            }
            assertThatThrownBy(() -> escenario.supresion.suprimir(laura.id(), "otra"))
                    .isInstanceOf(CuentaBloqueadaException.class);

            assertThat(laura.estado()).isEqualTo(EstadoCuenta.ACTIVA);
            assertThat(escenario.supresionesAvisadas).isEmpty();
        }

        @Test
        @DisplayName("solo el turista suprime en este PR; una cuenta ya suprimida tampoco (DN-31)")
        void soloTuristaActivo() {
            Usuario guia = escenario.registrar.registrar(new RegistrarUsuarioCommand(Rol.PRESTADOR, "Guía",
                    "guia@correo.co", "Minca2026", null, true, "v1"));
            assertThatThrownBy(() -> escenario.supresion.suprimir(guia.id(), "Minca2026"))
                    .isInstanceOf(CuentaNoEncontradaException.class);

            escenario.supresion.suprimir(laura.id(), "Playa2026");
            assertThatThrownBy(() -> escenario.supresion.suprimir(laura.id(), "Playa2026"))
                    .isInstanceOf(CuentaNoEncontradaException.class);
        }
    }
}
