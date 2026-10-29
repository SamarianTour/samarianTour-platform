package co.edu.unimagdalena.samariantour.nucleo.cuentas.aplicacion;

import co.edu.unimagdalena.samariantour.nucleo.cuentas.dominio.excepcion.CredencialesInvalidasException;
import co.edu.unimagdalena.samariantour.nucleo.cuentas.dominio.excepcion.CuentaBloqueadaException;
import co.edu.unimagdalena.samariantour.nucleo.cuentas.dominio.excepcion.SesionInvalidaException;
import co.edu.unimagdalena.samariantour.nucleo.cuentas.dominio.modelo.Rol;
import co.edu.unimagdalena.samariantour.nucleo.cuentas.dominio.modelo.Sesion;
import co.edu.unimagdalena.samariantour.nucleo.cuentas.dominio.modelo.SesionIniciada;
import co.edu.unimagdalena.samariantour.nucleo.cuentas.dominio.modelo.Usuario;
import co.edu.unimagdalena.samariantour.nucleo.cuentas.dominio.puerto.entrada.comando.RegistrarUsuarioCommand;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static co.edu.unimagdalena.samariantour.nucleo.cuentas.CuentasDePrueba.AHORA;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Iniciar, renovar y cerrar la sesión (RF-04 a RF-06, DN-29). */
class SesionServiceTest {

    private final EscenarioCuentas escenario = new EscenarioCuentas();
    private Usuario laura;

    @BeforeEach
    void registrar() {
        laura = escenario.registrar.registrar(new RegistrarUsuarioCommand(Rol.TURISTA, "Laura Gómez",
                "laura@correo.co", "Playa2026", "+573001234567", true, "v1"));
    }

    @Nested
    @DisplayName("Inicio de sesión (RF-04, RF-05)")
    class Inicio {

        @Test
        @DisplayName("con el correo en cualquier mayúscula y la contraseña da los dos tokens y abre la sesión")
        void correcto() {
            SesionIniciada sesion = escenario.iniciarSesion.iniciar(" LAURA@correo.co ", "Playa2026");

            assertThat(sesion.usuario().id()).isEqualTo(laura.id());
            assertThat(sesion.acceso().valor()).isEqualTo("jwt:" + laura.id() + ":" + sesion.renovacion().id());
            assertThat(sesion.acceso().expiraEn()).isEqualTo(AHORA.plus(Duration.ofMinutes(15)));
            assertThat(sesion.renovacionExpiraEn()).isEqualTo(AHORA.plus(Duration.ofDays(7)));
            Sesion guardada = escenario.sesiones.get(sesion.renovacion().id());
            assertThat(guardada.hashRenovacion()).isEqualTo("h:" + sesion.renovacion().secreto());
        }

        @Test
        @DisplayName("un correo que no existe y una contraseña equivocada responden lo mismo")
        void credencialesInvalidas() {
            assertThatThrownBy(() -> escenario.iniciarSesion.iniciar("nadie@correo.co", "Playa2026"))
                    .isInstanceOf(CredencialesInvalidasException.class);
            assertThatThrownBy(() -> escenario.iniciarSesion.iniciar("laura@correo.co", "otra"))
                    .isInstanceOf(CredencialesInvalidasException.class);
            assertThat(laura.intentosFallidos()).isEqualTo(1);
            assertThat(escenario.sesiones).isEmpty();
        }

        @Test
        @DisplayName("el quinto fallo bloquea 15 minutos: ni la contraseña correcta entra hasta que pase")
        void bloqueo() {
            for (int i = 0; i < 4; i++) {
                assertThatThrownBy(() -> escenario.iniciarSesion.iniciar("laura@correo.co", "otra"))
                        .isInstanceOf(CredencialesInvalidasException.class);
            }
            assertThatThrownBy(() -> escenario.iniciarSesion.iniciar("laura@correo.co", "otra"))
                    .isInstanceOf(CuentaBloqueadaException.class);
            assertThatThrownBy(() -> escenario.iniciarSesion.iniciar("laura@correo.co", "Playa2026"))
                    .isInstanceOf(CuentaBloqueadaException.class);

            escenario.ahora = AHORA.plus(Duration.ofMinutes(15));
            assertThat(escenario.iniciarSesion.iniciar("laura@correo.co", "Playa2026").usuario().id())
                    .isEqualTo(laura.id());
            assertThat(laura.bloqueadaHasta()).isNull();
        }
    }

    @Nested
    @DisplayName("Renovación y cierre (RF-04, RF-06)")
    class Renovacion {

        private SesionIniciada inicial;

        @BeforeEach
        void iniciar() {
            inicial = escenario.iniciarSesion.iniciar("laura@correo.co", "Playa2026");
        }

        @Test
        @DisplayName("renovar da tokens nuevos en la misma sesión y el token usado deja de servir")
        void renovar() {
            escenario.ahora = AHORA.plus(Duration.ofDays(6));
            SesionIniciada renovada = escenario.renovarSesion.renovar(inicial.renovacion().valor());

            assertThat(renovada.renovacion().id()).isEqualTo(inicial.renovacion().id());
            assertThat(renovada.renovacion().secreto()).isNotEqualTo(inicial.renovacion().secreto());
            assertThat(renovada.renovacionExpiraEn()).isEqualTo(AHORA.plus(Duration.ofDays(13)));
            assertThat(escenario.renovarSesion.renovar(renovada.renovacion().valor())).isNotNull();
        }

        @Test
        @DisplayName("un token ya usado que vuelve a llegar cierra la sesión: tampoco sirve el más reciente")
        void reuso() {
            SesionIniciada renovada = escenario.renovarSesion.renovar(inicial.renovacion().valor());

            assertThatThrownBy(() -> escenario.renovarSesion.renovar(inicial.renovacion().valor()))
                    .isInstanceOf(SesionInvalidaException.class);
            assertThatThrownBy(() -> escenario.renovarSesion.renovar(renovada.renovacion().valor()))
                    .isInstanceOf(SesionInvalidaException.class);
        }

        @Test
        @DisplayName("después de 7 días sin usarlo, o con un texto cualquiera, no renueva")
        void vencidoOMalFormado() {
            escenario.ahora = AHORA.plus(Duration.ofDays(7));
            assertThatThrownBy(() -> escenario.renovarSesion.renovar(inicial.renovacion().valor()))
                    .isInstanceOf(SesionInvalidaException.class);
            assertThatThrownBy(() -> escenario.renovarSesion.renovar("no-es-un-token"))
                    .isInstanceOf(SesionInvalidaException.class);
            assertThatThrownBy(() -> escenario.renovarSesion.renovar(null))
                    .isInstanceOf(SesionInvalidaException.class);
        }

        @Test
        @DisplayName("cerrar revoca la sesión (RF-06); con un token que no sirve no falla ni cierra nada")
        void cerrar() {
            escenario.cerrarSesion.cerrar(inicial.renovacion().id() + ".secreto-falso");
            assertThat(escenario.sesiones.get(inicial.renovacion().id()).revocadaEn()).isNull();

            escenario.cerrarSesion.cerrar(inicial.renovacion().valor());

            assertThat(escenario.sesiones.get(inicial.renovacion().id()).revocadaEn()).isEqualTo(AHORA);
            assertThatThrownBy(() -> escenario.renovarSesion.renovar(inicial.renovacion().valor()))
                    .isInstanceOf(SesionInvalidaException.class);
        }
    }
}
