package co.edu.unimagdalena.samariantour.catalogo.catalogo.aplicacion;

import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.Oferta;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.puerto.entrada.comando.DecisionModeracion;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.puerto.entrada.comando.EditarOfertaCommand;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.puerto.entrada.comando.RetirarOfertaCommand;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static co.edu.unimagdalena.samariantour.catalogo.catalogo.aplicacion.EscenarioAplicacion.AGENCIA;
import static co.edu.unimagdalena.samariantour.catalogo.catalogo.aplicacion.EscenarioAplicacion.actividad;
import static co.edu.unimagdalena.samariantour.catalogo.catalogo.aplicacion.EscenarioAplicacion.nombrePublicado;
import static co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.DatosDePrueba.PRESTADOR;
import static co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.DatosDePrueba.ZONA_ACTIVA;
import static org.assertj.core.api.Assertions.assertThat;

/** El flujo de la demostración, recorriendo los cinco casos de uso juntos. */
class FlujoDemostracionTest {

    private final EscenarioAplicacion escenario = new EscenarioAplicacion();

    @Test
    @DisplayName("[Demostración] habilitar → publicar → no aparece → aprobar → aparece → editar → sigue la "
            + "versión aprobada → aprobar edición → RNT inactivo → desaparece → retirar")
    void flujoCompleto() {
        escenario.habilitar(PRESTADOR, true, 1);
        Oferta oferta = escenario.publicar(AGENCIA, actividad("Caminata a Cabo San Juan", "120000", ZONA_ACTIVA));
        assertThat(escenario.buscarTodas().contenido()).isEmpty();

        escenario.moderar(oferta.id(), 1, DecisionModeracion.APROBAR);
        assertThat(escenario.buscarTodas().contenido()).extracting(Oferta::id).containsExactly(oferta.id());

        escenario.gestion.editar(new EditarOfertaCommand(oferta.id(), AGENCIA,
                actividad("Caminata editada", "130000", ZONA_ACTIVA)));
        assertThat(nombrePublicado(escenario.ficha.consultar(oferta.id()))).isEqualTo("Caminata a Cabo San Juan");

        escenario.moderar(oferta.id(), 2, DecisionModeracion.APROBAR);
        assertThat(nombrePublicado(escenario.ficha.consultar(oferta.id()))).isEqualTo("Caminata editada");

        escenario.habilitar(PRESTADOR, false, 2);
        assertThat(escenario.buscarTodas().contenido()).isEmpty();

        escenario.gestion.retirar(new RetirarOfertaCommand(oferta.id(), PRESTADOR));
        assertThat(escenario.guardada(oferta.id()).retirada()).isTrue();
    }
}
