package co.edu.unimagdalena.samariantour.catalogo.catalogo.aplicacion;

import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.excepcion.OfertaNoEncontradaException;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.Oferta;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.Paginacion;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.puerto.entrada.comando.DecisionModeracion;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.puerto.entrada.comando.EditarOfertaCommand;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.puerto.entrada.comando.RetirarOfertaCommand;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static co.edu.unimagdalena.samariantour.catalogo.catalogo.aplicacion.EscenarioAplicacion.AGENCIA;
import static co.edu.unimagdalena.samariantour.catalogo.catalogo.aplicacion.EscenarioAplicacion.actividad;
import static co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.DatosDePrueba.PRESTADOR;
import static co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.DatosDePrueba.ZONA_ACTIVA;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ListarPendientesModeracionServiceTest {

    private final EscenarioAplicacion escenario = new EscenarioAplicacion();
    private final ListarPendientesModeracionService servicio = escenario.moderacion;

    @Test
    @DisplayName("lista las ofertas nuevas y las ediciones que esperan decisión, pero no las aprobadas ni rechazadas")
    void listarOfertas() {
        escenario.habilitar(PRESTADOR, true, 1);
        Oferta nueva = escenario.publicar(AGENCIA, actividad("Nueva", "50000", ZONA_ACTIVA));
        Oferta editada = escenario.ofertaPublicada("Editada", "60000", ZONA_ACTIVA);
        escenario.gestion.editar(new EditarOfertaCommand(editada.id(), AGENCIA,
                actividad("Editada con otro precio", "65000", ZONA_ACTIVA)));
        escenario.ofertaPublicada("Aprobada", "70000", ZONA_ACTIVA);
        Oferta rechazada = escenario.publicar(AGENCIA, actividad("Rechazada", "80000", ZONA_ACTIVA));
        escenario.moderar(rechazada.id(), 1, DecisionModeracion.RECHAZAR);

        assertThat(servicio.listarOfertas(Paginacion.de(null, null)).contenido()).extracting(Oferta::id)
                .containsExactlyInAnyOrder(nueva.id(), editada.id());
    }

    @Test
    @DisplayName("el núcleo consulta una oferta en moderación con su versión pendiente")
    void consultarOferta() {
        escenario.habilitar(PRESTADOR, true, 1);
        Oferta nueva = escenario.publicar(AGENCIA, actividad("Nueva", "50000", ZONA_ACTIVA));

        Oferta consultada = servicio.consultarOferta(nueva.id());

        assertThat(consultada.prestadorId()).isEqualTo(PRESTADOR);
        assertThat(consultada.pendiente().orElseThrow().numero()).isEqualTo(1);
    }

    @Test
    @DisplayName("una oferta inexistente, ya aprobada o retirada no está en moderación: 404")
    void consultarFueraDeModeracion() {
        Oferta aprobada = escenario.ofertaPublicada("Aprobada", "50000", ZONA_ACTIVA);
        Oferta retirada = escenario.publicar(AGENCIA, actividad("Retirada", "50000", ZONA_ACTIVA));
        escenario.gestion.retirar(new RetirarOfertaCommand(retirada.id(), PRESTADOR));

        for (String id : new String[] {"no-existe", aprobada.id(), retirada.id()}) {
            assertThatThrownBy(() -> servicio.consultarOferta(id)).isInstanceOf(OfertaNoEncontradaException.class);
        }
    }
}
