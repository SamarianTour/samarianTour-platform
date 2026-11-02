package co.edu.unimagdalena.samariantour.nucleo.administracion.aplicacion;

import co.edu.unimagdalena.samariantour.nucleo.administracion.dominio.excepcion.OperadorNoValidoException;
import co.edu.unimagdalena.samariantour.nucleo.administracion.dominio.excepcion.PrestadorNoRegistradoException;
import co.edu.unimagdalena.samariantour.nucleo.administracion.dominio.modelo.CuentaInterna;
import co.edu.unimagdalena.samariantour.nucleo.administracion.dominio.modelo.PrestadorRegistrado;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static co.edu.unimagdalena.samariantour.nucleo.administracion.aplicacion.EscenarioCuentasInternas.ADMINISTRADOR;
import static co.edu.unimagdalena.samariantour.nucleo.administracion.aplicacion.EscenarioCuentasInternas.GESTOR;
import static co.edu.unimagdalena.samariantour.nucleo.administracion.aplicacion.EscenarioCuentasInternas.OPERADOR;
import static co.edu.unimagdalena.samariantour.nucleo.administracion.aplicacion.EscenarioCuentasInternas.SIN_OPERADOR;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** El operador de cada prestador (RF-32, RF-81, DN-39). */
class AsignarOperadorPrestadorServiceTest {

    private final EscenarioCuentasInternas escenario = new EscenarioCuentasInternas();

    @Test
    @DisplayName("la lista de los que faltan por asignar se vacía al asignarles un operador")
    void asignarLosQueFaltan() {
        assertThat(escenario.asignar.prestadores(true)).extracting(PrestadorRegistrado::id)
                .containsExactly(SIN_OPERADOR);

        PrestadorRegistrado asignado = escenario.asignar.asignar(SIN_OPERADOR, OPERADOR);

        assertThat(asignado.operadorId()).isEqualTo(OPERADOR);
        assertThat(escenario.asignar.prestadores(true)).isEmpty();
        assertThat(escenario.asignar.prestadores(false)).hasSize(2);
    }

    @Test
    @DisplayName("un gestor, un administrador o una cuenta desconocida no son operadores válidos")
    void noEsOperador() {
        for (UUID cuenta : new UUID[] {GESTOR, ADMINISTRADOR, UUID.randomUUID()}) {
            assertThatThrownBy(() -> escenario.asignar.asignar(SIN_OPERADOR, cuenta))
                    .isInstanceOf(OperadorNoValidoException.class);
        }
        assertThat(escenario.prestadores.get(SIN_OPERADOR).operadorId()).isNull();
    }

    @Test
    @DisplayName("un operador desactivado tampoco: sus solicitudes no las atendería nadie")
    void operadorDesactivado() {
        CuentaInterna operador = escenario.cuentas.get(OPERADOR);
        escenario.cuentas.put(OPERADOR, new CuentaInterna(OPERADOR, operador.rol(), operador.nombre(),
                operador.correo(), "DESACTIVADA", true, operador.registradaEn()));

        assertThatThrownBy(() -> escenario.asignar.asignar(SIN_OPERADOR, OPERADOR))
                .isInstanceOf(OperadorNoValidoException.class);
    }

    @Test
    @DisplayName("un prestador que no registró su RNT es 404")
    void prestadorNoRegistrado() {
        assertThatThrownBy(() -> escenario.asignar.asignar(UUID.randomUUID(), OPERADOR))
                .isInstanceOf(PrestadorNoRegistradoException.class);
    }
}
