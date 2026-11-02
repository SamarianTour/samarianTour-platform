package co.edu.unimagdalena.samariantour.nucleo.cuentas.dominio.modelo;

import co.edu.unimagdalena.samariantour.nucleo.cuentas.dominio.excepcion.DatoInvalidoException;
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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

    @Nested
    @DisplayName("Cuentas internas (RF-81)")
    class CuentasInternas {

        @Test
        @DisplayName("la crea el administrador: nace activa y sin contraseña, con la subclase de su rol")
        void crear() {
            Usuario operador = Usuario.crearInterna(UUID.randomUUID(), Rol.OPERADOR_DISPONIBILIDAD, " Valentina ",
                    "Valentina@SamarianTour.co", AHORA);

            assertThat(operador).isInstanceOf(OperadorDisponibilidad.class);
            assertThat(operador.nombre()).isEqualTo("Valentina");
            assertThat(operador.correo()).isEqualTo("valentina@samariantour.co");
            assertThat(operador.estaActiva()).isTrue();
            assertThat(operador.tieneContrasena()).isFalse();
            assertThat(operador.autenticar("", HASHER)).isFalse();
            assertThat(operador.aceptaciones()).isEmpty();
        }

        @Test
        @DisplayName("el turista y el prestador no son cuentas internas: se registran solos")
        void soloRolesInternos() {
            for (Rol rol : new Rol[] {Rol.TURISTA, Rol.PRESTADOR, null}) {
                assertThatThrownBy(() -> Usuario.crearInterna(UUID.randomUUID(), rol, "X", "x@correo.co", AHORA))
                        .isInstanceOf(DatoInvalidoException.class).hasMessageStartingWith("rol: ");
            }
        }

        @Test
        @DisplayName("desactivarla y reactivarla cambia el estado una vez; repetirlo no cambia nada")
        void desactivarYReactivar() {
            Usuario gestor = cuenta(Rol.GESTOR_DESTINO, "g@correo.co");

            assertThat(gestor.desactivar()).isTrue();
            assertThat(gestor.estado()).isEqualTo(EstadoCuenta.DESACTIVADA);
            assertThat(gestor.desactivar()).isFalse();

            assertThat(gestor.reactivar()).isTrue();
            assertThat(gestor.estaActiva()).isTrue();
            assertThat(gestor.reactivar()).isFalse();
            assertThat(gestor.autenticar("Clave1234", HASHER)).isTrue();
        }

        @Test
        @DisplayName("el turista no se desactiva: pide la supresión de sus datos (RF-13)")
        void turistaNoSeDesactiva() {
            Usuario turista = cuenta(Rol.TURISTA, "t@correo.co");

            assertThatThrownBy(turista::desactivar).isInstanceOf(IllegalStateException.class);
            assertThat(turista.estaActiva()).isTrue();
        }
    }

    @Test
    @DisplayName("el turista solo envía solicitudes con el WhatsApp verificado (RF-30)")

    void turistaVerificado() {
        assertThat(turista("t@correo.co", "+573001234567", true).puedeEnviarSolicitudes()).isTrue();
        assertThat(turista("t@correo.co", "+573001234567", false).puedeEnviarSolicitudes()).isFalse();
        assertThat(turista("t@correo.co", null, true).whatsappVerificado()).isFalse();
    }
}
