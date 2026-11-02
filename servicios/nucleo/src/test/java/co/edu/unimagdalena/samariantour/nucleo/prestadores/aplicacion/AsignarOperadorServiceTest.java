package co.edu.unimagdalena.samariantour.nucleo.prestadores.aplicacion;

import co.edu.unimagdalena.samariantour.nucleo.prestadores.dominio.modelo.Prestador;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static co.edu.unimagdalena.samariantour.nucleo.prestadores.PrestadoresDePrueba.AGENCIA;
import static co.edu.unimagdalena.samariantour.nucleo.prestadores.PrestadoresDePrueba.AHORA;
import static co.edu.unimagdalena.samariantour.nucleo.prestadores.PrestadoresDePrueba.GUIA;
import static co.edu.unimagdalena.samariantour.nucleo.prestadores.PrestadoresDePrueba.agenciaActiva;
import static co.edu.unimagdalena.samariantour.nucleo.prestadores.PrestadoresDePrueba.guiaActivo;
import static org.assertj.core.api.Assertions.assertThat;

/** El operador de cada prestador (RF-32, RF-81, DN-39). */
class AsignarOperadorServiceTest {

    private final EscenarioPrestadores escenario = new EscenarioPrestadores();
    private final UUID valentina = UUID.randomUUID();
    private final UUID carlos = UUID.randomUUID();

    private Prestador registrado(String numero, boolean guia, long segundos) {
        Prestador prestador = Prestador.registrar(UUID.randomUUID(), numero, guia ? GUIA : AGENCIA,
                guia ? guiaActivo(numero) : agenciaActiva(numero), AHORA.plusSeconds(segundos));
        escenario.repositorio.insertar(prestador);
        return prestador;
    }

    @Test
    @DisplayName("asignarlo lo guarda y deja en la auditoría el operador anterior")
    void asignar() {
        Prestador agencia = registrado("910001", false, 0);

        escenario.asignarOperador.asignar(agencia.id(), valentina);
        escenario.asignarOperador.asignar(agencia.id(), carlos);

        assertThat(escenario.prestadores.get(agencia.id()).operadorId()).isEqualTo(carlos);
        assertThat(escenario.auditoria).containsExactly(
                agencia.id() + " OPERADOR_ASIGNADO " + valentina + " (antes sin operador)",
                agencia.id() + " OPERADOR_ASIGNADO " + carlos + " (antes " + valentina + ")");
    }

    @Test
    @DisplayName("el mismo operador no cambia nada ni queda en la auditoría")
    void mismoOperador() {
        Prestador agencia = registrado("910001", false, 0);
        escenario.asignarOperador.asignar(agencia.id(), valentina);

        assertThat(escenario.asignarOperador.asignar(agencia.id(), valentina)).get()
                .extracting(Prestador::operadorId).isEqualTo(valentina);
        assertThat(escenario.auditoria).hasSize(1);
    }

    @Test
    @DisplayName("un prestador que no registró su RNT no se encuentra")
    void noRegistrado() {
        assertThat(escenario.asignarOperador.asignar(UUID.randomUUID(), valentina)).isEmpty();
        assertThat(escenario.auditoria).isEmpty();
    }

    @Test
    @DisplayName("la lista va del registrado hace más tiempo al más reciente, y puede traer solo los sin operador")
    void lista() {
        Prestador guia = registrado("910002", true, 60);
        Prestador agencia = registrado("910001", false, 0);
        Prestador otraAgencia = registrado("910003", false, 120);
        escenario.asignarOperador.asignar(guia.id(), valentina);
        escenario.asignarOperador.asignar(otraAgencia.id(), valentina);

        assertThat(escenario.consultar.todos(false)).extracting(Prestador::id)
                .containsExactly(agencia.id(), guia.id(), otraAgencia.id());
        assertThat(escenario.consultar.todos(true)).extracting(Prestador::id).containsExactly(agencia.id());
        assertThat(escenario.consultar.asignadosA(valentina)).isEqualTo(2);
        assertThat(escenario.consultar.asignadosA(carlos)).isZero();
    }
}
