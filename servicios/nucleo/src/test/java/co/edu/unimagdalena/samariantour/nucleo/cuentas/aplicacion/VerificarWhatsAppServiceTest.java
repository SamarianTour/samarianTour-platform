package co.edu.unimagdalena.samariantour.nucleo.cuentas.aplicacion;

import co.edu.unimagdalena.samariantour.nucleo.cuentas.dominio.excepcion.CodigoIncorrectoException;
import co.edu.unimagdalena.samariantour.nucleo.cuentas.dominio.excepcion.CodigoNoVigenteException;
import co.edu.unimagdalena.samariantour.nucleo.cuentas.dominio.excepcion.CuentaNoEncontradaException;
import co.edu.unimagdalena.samariantour.nucleo.cuentas.dominio.modelo.Rol;
import co.edu.unimagdalena.samariantour.nucleo.cuentas.dominio.modelo.Turista;
import co.edu.unimagdalena.samariantour.nucleo.cuentas.dominio.modelo.Usuario;
import co.edu.unimagdalena.samariantour.nucleo.cuentas.dominio.puerto.entrada.comando.RegistrarUsuarioCommand;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.UUID;

import static co.edu.unimagdalena.samariantour.nucleo.cuentas.CuentasDePrueba.AHORA;
import static co.edu.unimagdalena.samariantour.nucleo.cuentas.aplicacion.EscenarioCuentas.CODIGO;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** La verificación del WhatsApp del turista (RF-03, RF-08, DN-28). */
class VerificarWhatsAppServiceTest {

    private final EscenarioCuentas escenario = new EscenarioCuentas();
    private UUID turistaId;

    @BeforeEach
    void registrar() {
        turistaId = escenario.registrar.registrar(new RegistrarUsuarioCommand(Rol.TURISTA, "Laura Gómez",
                "laura@correo.co", "Playa2026", "+573001234567", true, "v1")).id();
    }

    @Test
    @DisplayName("con el código correcto queda verificado y puede enviar solicitudes; el código no sirve otra vez")
    void verificar() {
        Turista turista = escenario.verificarWhatsApp.verificar(turistaId, " " + CODIGO + " ");

        assertThat(turista.puedeEnviarSolicitudes()).isTrue();
        assertThat(escenario.codigos.getFirst().esValido(AHORA)).isFalse();
        assertThat(escenario.verificarWhatsApp.verificar(turistaId, "000000").whatsappVerificado()).isTrue();
    }

    @Test
    @DisplayName("cada código equivocado cuenta; al tercero queda invalidado y hay que pedir otro")
    void tresIntentos() {
        assertThatThrownBy(() -> escenario.verificarWhatsApp.verificar(turistaId, "111111"))
                .isInstanceOf(CodigoIncorrectoException.class).hasMessageContaining("quedan 2");
        assertThatThrownBy(() -> escenario.verificarWhatsApp.verificar(turistaId, "111111"))
                .isInstanceOf(CodigoIncorrectoException.class);
        assertThatThrownBy(() -> escenario.verificarWhatsApp.verificar(turistaId, "111111"))
                .isInstanceOf(CodigoIncorrectoException.class)
                .satisfies(e -> assertThat(((CodigoIncorrectoException) e).intentosRestantes()).isZero());

        assertThatThrownBy(() -> escenario.verificarWhatsApp.verificar(turistaId, CODIGO))
                .isInstanceOf(CodigoNoVigenteException.class);

        escenario.ahora = AHORA.plus(Duration.ofMinutes(1));
        escenario.verificarWhatsApp.enviarCodigo(turistaId);
        assertThat(escenario.verificarWhatsApp.verificar(turistaId, CODIGO).whatsappVerificado()).isTrue();
    }

    @Test
    @DisplayName("un código de más de 30 minutos ya no sirve (RF-08)")
    void vencido() {
        escenario.ahora = AHORA.plus(Duration.ofMinutes(30));

        assertThatThrownBy(() -> escenario.verificarWhatsApp.verificar(turistaId, CODIGO))
                .isInstanceOf(CodigoNoVigenteException.class);
    }

    @Test
    @DisplayName("pedir otro código envía uno nuevo; ya verificado no envía nada")
    void reenviar() {
        escenario.verificarWhatsApp.enviarCodigo(turistaId);
        assertThat(escenario.whatsappsEnviados).hasSize(2);

        escenario.verificarWhatsApp.verificar(turistaId, CODIGO);
        escenario.verificarWhatsApp.enviarCodigo(turistaId);
        assertThat(escenario.whatsappsEnviados).hasSize(2);
    }

    @Test
    @DisplayName("solo un turista registrado verifica: otra cuenta o una que no existe responde no encontrada")
    void soloTurista() {
        Usuario prestador = escenario.registrar.registrar(new RegistrarUsuarioCommand(Rol.PRESTADOR, "Guía",
                "guia@correo.co", "Minca2026", null, true, "v1"));

        assertThatThrownBy(() -> escenario.verificarWhatsApp.verificar(prestador.id(), CODIGO))
                .isInstanceOf(CuentaNoEncontradaException.class);
        assertThatThrownBy(() -> escenario.verificarWhatsApp.enviarCodigo(UUID.randomUUID()))
                .isInstanceOf(CuentaNoEncontradaException.class);
    }
}
