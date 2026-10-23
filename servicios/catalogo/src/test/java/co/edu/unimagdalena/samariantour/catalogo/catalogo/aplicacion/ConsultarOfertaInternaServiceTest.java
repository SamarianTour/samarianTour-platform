package co.edu.unimagdalena.samariantour.catalogo.catalogo.aplicacion;

import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.excepcion.OfertaNoEncontradaException;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.Oferta;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.puerto.entrada.comando.DecisionModeracion;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.puerto.entrada.comando.RetirarOfertaCommand;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static co.edu.unimagdalena.samariantour.catalogo.catalogo.aplicacion.EscenarioAplicacion.AGENCIA;
import static co.edu.unimagdalena.samariantour.catalogo.catalogo.aplicacion.EscenarioAplicacion.actividad;
import static co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.DatosDePrueba.PRESTADOR;
import static co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.DatosDePrueba.ZONA_ACTIVA;
import static co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.DatosDePrueba.paquete;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ConsultarOfertaInternaServiceTest {

    private final EscenarioAplicacion escenario = new EscenarioAplicacion();
    private final ConsultarOfertaInternaService servicio = escenario.consultaInterna;

    @Test
    @DisplayName("devuelve la oferta visible que el turista puede pedir (RF-30)")
    void visible() {
        Oferta oferta = escenario.ofertaPublicada("Tour", "50000", ZONA_ACTIVA);

        assertThat(servicio.consultar(oferta.id()).id()).isEqualTo(oferta.id());
    }

    @Test
    @DisplayName("una oferta inexistente, sin versión aprobada o retirada no se puede pedir")
    void noVisible() {
        escenario.habilitar(PRESTADOR, true, 1);
        Oferta enModeracion = escenario.publicar(AGENCIA, actividad("Sin aprobar", "50000", ZONA_ACTIVA));
        Oferta retirada = escenario.ofertaPublicada("Retirada", "50000", ZONA_ACTIVA);
        escenario.gestion.retirar(new RetirarOfertaCommand(retirada.id(), PRESTADOR));

        for (String id : new String[] {"no-existe", enModeracion.id(), retirada.id()}) {
            assertThatThrownBy(() -> servicio.consultar(id)).isInstanceOf(OfertaNoEncontradaException.class);
        }
    }

    @Test
    @DisplayName("con el RNT del prestador inactivo, sus ofertas no se pueden pedir (RF-77)")
    void rntInactivo() {
        Oferta oferta = escenario.ofertaPublicada("Tour", "50000", ZONA_ACTIVA);

        escenario.habilitar(PRESTADOR, false, 2);

        assertThatThrownBy(() -> servicio.consultar(oferta.id())).isInstanceOf(OfertaNoEncontradaException.class);
    }

    @Test
    @DisplayName("un paquete con una actividad retirada no se puede pedir (RF-22)")
    void paqueteConActividadRetirada() {
        Oferta caminata = escenario.ofertaPublicada("Caminata", "50000", ZONA_ACTIVA);
        Oferta kayak = escenario.ofertaPublicada("Kayak", "70000", ZONA_ACTIVA);
        Oferta paquete = escenario.publicar(AGENCIA, paquete(caminata.id(), kayak.id()));
        escenario.moderar(paquete.id(), 1, DecisionModeracion.APROBAR);
        assertThat(servicio.consultar(paquete.id()).id()).isEqualTo(paquete.id());

        escenario.gestion.retirar(new RetirarOfertaCommand(kayak.id(), PRESTADOR));

        assertThatThrownBy(() -> servicio.consultar(paquete.id())).isInstanceOf(OfertaNoEncontradaException.class);
    }
}
