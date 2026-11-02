package co.edu.unimagdalena.samariantour.nucleo.cuentas.aplicacion;

import co.edu.unimagdalena.samariantour.nucleo.cuentas.dominio.excepcion.CodigoNoVigenteException;
import co.edu.unimagdalena.samariantour.nucleo.cuentas.dominio.excepcion.CorreoYaRegistradoException;
import co.edu.unimagdalena.samariantour.nucleo.cuentas.dominio.excepcion.CredencialesInvalidasException;
import co.edu.unimagdalena.samariantour.nucleo.cuentas.dominio.excepcion.CuentaDesactivadaException;
import co.edu.unimagdalena.samariantour.nucleo.cuentas.dominio.excepcion.CuentaNoEncontradaException;
import co.edu.unimagdalena.samariantour.nucleo.cuentas.dominio.excepcion.DatoInvalidoException;
import co.edu.unimagdalena.samariantour.nucleo.cuentas.dominio.modelo.PropositoCodigo;
import co.edu.unimagdalena.samariantour.nucleo.cuentas.dominio.modelo.Rol;
import co.edu.unimagdalena.samariantour.nucleo.cuentas.dominio.modelo.SesionIniciada;
import co.edu.unimagdalena.samariantour.nucleo.cuentas.dominio.modelo.Usuario;
import co.edu.unimagdalena.samariantour.nucleo.cuentas.dominio.puerto.entrada.comando.CrearCuentaInternaCommand;
import co.edu.unimagdalena.samariantour.nucleo.cuentas.dominio.puerto.entrada.comando.RegistrarUsuarioCommand;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static co.edu.unimagdalena.samariantour.nucleo.cuentas.CuentasDePrueba.AHORA;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Las cuentas internas que crea y da de baja el administrador (RF-81, DN-40). */
class CuentasInternasServiceTest {

    private final EscenarioCuentas escenario = new EscenarioCuentas();

    private Usuario crearOperador() {
        return escenario.crearInterna.crear(new CrearCuentaInternaCommand(Rol.OPERADOR_DISPONIBILIDAD,
                "Valentina Ríos", "valentina@correo.co"));
    }

    @Test
    @DisplayName("la cuenta nace sin contraseña; con el enlace de la invitación la pone e inicia sesión")
    void invitacion() {
        Usuario operador = crearOperador();

        assertThat(operador.tieneContrasena()).isFalse();
        assertThat(escenario.invitacionesEnviadas).singleElement().asString().startsWith("valentina@correo.co ");
        assertThat(escenario.codigos).singleElement()
                .satisfies(codigo -> assertThat(codigo.proposito()).isEqualTo(PropositoCodigo.ESTABLECER_CONTRASENA));
        assertThatThrownBy(() -> escenario.iniciarSesion.iniciar("valentina@correo.co", "Caribe2026"))
                .isInstanceOf(CredencialesInvalidasException.class);

        escenario.restablecerContrasena.restablecer(escenario.ultimaInvitacion(), "Caribe2026");

        assertThat(escenario.iniciarSesion.iniciar("valentina@correo.co", "Caribe2026").usuario().id())
                .isEqualTo(operador.id());
    }

    @Test
    @DisplayName("la invitación vale 72 horas; si vence, el restablecimiento de siempre manda otro enlace")
    void invitacionVencida() {
        crearOperador();
        String invitacion = escenario.ultimaInvitacion();
        escenario.ahora = AHORA.plus(Duration.ofHours(72));

        assertThatThrownBy(() -> escenario.restablecerContrasena.restablecer(invitacion, "Caribe2026"))
                .isInstanceOf(CodigoNoVigenteException.class);

        escenario.solicitarRestablecimiento.solicitar("valentina@correo.co");
        escenario.restablecerContrasena.restablecer(escenario.ultimoEnlace(), "Caribe2026");
        assertThat(escenario.iniciarSesion.iniciar("valentina@correo.co", "Caribe2026")).isNotNull();
    }

    @Test
    @DisplayName("el correo no se repite con otra cuenta, y el turista y el prestador no son internos")
    void datosInvalidos() {
        escenario.registrar.registrar(new RegistrarUsuarioCommand(Rol.TURISTA, "Laura Gómez", "laura@correo.co",
                "Playa2026", "+573001234567", true, "v1"));

        assertThatThrownBy(() -> escenario.crearInterna.crear(new CrearCuentaInternaCommand(Rol.GESTOR_DESTINO,
                "Otra Laura", "LAURA@correo.co"))).isInstanceOf(CorreoYaRegistradoException.class);
        assertThatThrownBy(() -> escenario.crearInterna.crear(new CrearCuentaInternaCommand(Rol.PRESTADOR,
                "Agencia", "agencia@correo.co"))).isInstanceOf(DatoInvalidoException.class);
        assertThat(escenario.invitacionesEnviadas).isEmpty();
    }

    @Test
    @DisplayName("desactivada no inicia sesión y pierde sus sesiones; reactivada entra con la misma contraseña")
    void desactivarYReactivar() {
        Usuario operador = crearOperador();
        escenario.restablecerContrasena.restablecer(escenario.ultimaInvitacion(), "Caribe2026");
        SesionIniciada abierta = escenario.iniciarSesion.iniciar("valentina@correo.co", "Caribe2026");

        escenario.desactivarInterna.desactivar(operador.id());

        assertThat(escenario.sesiones.get(abierta.renovacion().id()).revocadaEn()).isEqualTo(AHORA);
        assertThatThrownBy(() -> escenario.iniciarSesion.iniciar("valentina@correo.co", "Caribe2026"))
                .isInstanceOf(CuentaDesactivadaException.class);
        assertThat(escenario.consultarInternas.esOperadorActivo(operador.id())).isFalse();

        escenario.desactivarInterna.reactivar(operador.id());

        assertThat(escenario.iniciarSesion.iniciar("valentina@correo.co", "Caribe2026")).isNotNull();
        assertThat(escenario.consultarInternas.esOperadorActivo(operador.id())).isTrue();
    }

    @Test
    @DisplayName("solo se desactivan las cuentas internas: un turista es como si no existiera")
    void soloInternas() {
        Usuario turista = escenario.registrar.registrar(new RegistrarUsuarioCommand(Rol.TURISTA, "Laura Gómez",
                "laura@correo.co", "Playa2026", "+573001234567", true, "v1"));

        assertThatThrownBy(() -> escenario.desactivarInterna.desactivar(turista.id()))
                .isInstanceOf(CuentaNoEncontradaException.class);
        assertThat(escenario.consultarInternas.interna(turista.id())).isEmpty();
    }

    @Test
    @DisplayName("la lista trae solo las internas, de la más antigua a la más reciente")
    void lista() {
        Usuario operador = crearOperador();
        escenario.ahora = AHORA.plusSeconds(60);
        Usuario gestor = escenario.crearInterna.crear(new CrearCuentaInternaCommand(Rol.GESTOR_DESTINO, "Gestor",
                "gestor@correo.co"));
        escenario.registrar.registrar(new RegistrarUsuarioCommand(Rol.TURISTA, "Laura Gómez", "laura@correo.co",
                "Playa2026", "+573001234567", true, "v1"));

        assertThat(escenario.consultarInternas.internas()).extracting(Usuario::id)
                .containsExactly(operador.id(), gestor.id());
        assertThat(escenario.consultarInternas.esOperadorActivo(gestor.id())).isFalse();
    }
}
