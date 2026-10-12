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

class ConsultarFichaOfertaServiceTest {

    private final EscenarioAplicacion escenario = new EscenarioAplicacion();
    private final ConsultarFichaOfertaService servicio = escenario.ficha;

    @Test
    @DisplayName("devuelve la oferta visible")
    void visible() {
        Oferta oferta = escenario.ofertaPublicada("Tour", "50000", ZONA_ACTIVA);

        assertThat(servicio.consultar(oferta.id()).id()).isEqualTo(oferta.id());
    }

    @Test
    @DisplayName("una oferta no visible responde igual que una inexistente")
    void noVisible() {
        escenario.habilitar(PRESTADOR, true, 1);
        Oferta enModeracion = escenario.publicar(AGENCIA, actividad("Tour", "50000", ZONA_ACTIVA));

        assertThatThrownBy(() -> servicio.consultar(enModeracion.id()))
                .isInstanceOf(OfertaNoEncontradaException.class);
        assertThatThrownBy(() -> servicio.consultar("no-existe"))
                .isInstanceOf(OfertaNoEncontradaException.class);
    }

    @Test
    @DisplayName("un paquete deja de ser visible cuando se retira una de sus actividades (RF-22)")
    void paqueteConActividadRetirada() {
        Oferta caminata = escenario.ofertaPublicada("Caminata", "50000", ZONA_ACTIVA);
        Oferta kayak = escenario.ofertaPublicada("Kayak", "70000", ZONA_ACTIVA);
        Oferta paquete = escenario.publicar(AGENCIA, paquete(caminata.id(), kayak.id()));
        escenario.moderar(paquete.id(), 1, DecisionModeracion.APROBAR);
        assertThat(servicio.consultar(paquete.id()).id()).isEqualTo(paquete.id());

        escenario.gestion.retirar(new RetirarOfertaCommand(kayak.id(), PRESTADOR));

        assertThatThrownBy(() -> servicio.consultar(paquete.id()))
                .isInstanceOf(OfertaNoEncontradaException.class);
        assertThat(servicio.consultar(caminata.id()).id()).isEqualTo(caminata.id());
    }
}
