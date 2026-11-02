package co.edu.unimagdalena.samariantour.nucleo.administracion.aplicacion;

import co.edu.unimagdalena.samariantour.nucleo.administracion.dominio.excepcion.CuentaInternaNoEncontradaException;
import co.edu.unimagdalena.samariantour.nucleo.administracion.dominio.excepcion.CuentaPropiaException;
import co.edu.unimagdalena.samariantour.nucleo.administracion.dominio.excepcion.OperadorConPrestadoresException;
import co.edu.unimagdalena.samariantour.nucleo.administracion.dominio.puerto.entrada.comando.CrearCuentaInternaCommand;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static co.edu.unimagdalena.samariantour.nucleo.administracion.aplicacion.EscenarioCuentasInternas.ADMINISTRADOR;
import static co.edu.unimagdalena.samariantour.nucleo.administracion.aplicacion.EscenarioCuentasInternas.AGENCIA;
import static co.edu.unimagdalena.samariantour.nucleo.administracion.aplicacion.EscenarioCuentasInternas.GESTOR;
import static co.edu.unimagdalena.samariantour.nucleo.administracion.aplicacion.EscenarioCuentasInternas.OPERADOR;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Las reglas de la administración al desactivar una cuenta interna (RF-81, DN-40). */
class GestionarCuentasInternasServiceTest {

    private final EscenarioCuentasInternas escenario = new EscenarioCuentasInternas();

    @Test
    @DisplayName("un operador con prestadores asignados no se desactiva; reasignados, sí")
    void operadorConPrestadores() {
        UUID otro = escenario.gestionar.crear(new CrearCuentaInternaCommand("OPERADOR_DISPONIBILIDAD", "Otro",
                "otro@correo.co")).id();

        assertThatThrownBy(() -> escenario.gestionar.desactivar(OPERADOR, ADMINISTRADOR))
                .isInstanceOf(OperadorConPrestadoresException.class)
                .hasMessageContaining("1 prestador asignado");
        assertThat(escenario.cuentas.get(OPERADOR).estado()).isEqualTo("ACTIVA");

        escenario.asignar.asignar(AGENCIA, otro);

        assertThat(escenario.gestionar.desactivar(OPERADOR, ADMINISTRADOR).estado()).isEqualTo("DESACTIVADA");
    }

    @Test
    @DisplayName("el administrador no puede desactivar su propia cuenta")
    void cuentaPropia() {
        assertThatThrownBy(() -> escenario.gestionar.desactivar(ADMINISTRADOR, ADMINISTRADOR))
                .isInstanceOf(CuentaPropiaException.class);
        assertThat(escenario.cuentas.get(ADMINISTRADOR).estado()).isEqualTo("ACTIVA");
    }

    @Test
    @DisplayName("un gestor se desactiva y se reactiva sin revisar prestadores")
    void gestor() {
        assertThat(escenario.gestionar.desactivar(GESTOR, ADMINISTRADOR).estado()).isEqualTo("DESACTIVADA");
        assertThat(escenario.gestionar.reactivar(GESTOR).estado()).isEqualTo("ACTIVA");
    }

    @Test
    @DisplayName("una cuenta que no es interna, o no existe, es 404")
    void noEncontrada() {
        UUID turista = UUID.randomUUID();

        assertThatThrownBy(() -> escenario.gestionar.desactivar(turista, ADMINISTRADOR))
                .isInstanceOf(CuentaInternaNoEncontradaException.class);
        assertThatThrownBy(() -> escenario.gestionar.reactivar(turista))
                .isInstanceOf(CuentaInternaNoEncontradaException.class);
    }
}
