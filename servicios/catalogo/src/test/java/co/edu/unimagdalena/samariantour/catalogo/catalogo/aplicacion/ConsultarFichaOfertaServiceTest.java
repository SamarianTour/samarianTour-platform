package co.edu.unimagdalena.samariantour.catalogo.catalogo.aplicacion;

import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.excepcion.OfertaNoEncontradaException;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.Oferta;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static co.edu.unimagdalena.samariantour.catalogo.catalogo.aplicacion.EscenarioAplicacion.AGENCIA;
import static co.edu.unimagdalena.samariantour.catalogo.catalogo.aplicacion.EscenarioAplicacion.actividad;
import static co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.DatosDePrueba.PRESTADOR;
import static co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.DatosDePrueba.ZONA_ACTIVA;
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
}
