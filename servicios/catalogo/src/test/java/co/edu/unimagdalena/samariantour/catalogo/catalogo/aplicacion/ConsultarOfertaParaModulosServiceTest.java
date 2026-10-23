package co.edu.unimagdalena.samariantour.catalogo.catalogo.aplicacion;

import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.Oferta;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.puerto.entrada.comando.DecisionModeracion;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.puerto.entrada.comando.EditarOfertaCommand;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.puerto.entrada.comando.RetirarOfertaCommand;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static co.edu.unimagdalena.samariantour.catalogo.catalogo.aplicacion.EscenarioAplicacion.AGENCIA;
import static co.edu.unimagdalena.samariantour.catalogo.catalogo.aplicacion.EscenarioAplicacion.actividad;
import static co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.DatosDePrueba.PRESTADOR;
import static co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.DatosDePrueba.ZONA_ACTIVA;
import static co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.DatosDePrueba.paquete;
import static org.assertj.core.api.Assertions.assertThat;

class ConsultarOfertaParaModulosServiceTest {

    private final EscenarioAplicacion escenario = new EscenarioAplicacion();
    private final ConsultarOfertaParaModulosService servicio = escenario.paraModulos;

    @Test
    @DisplayName("una oferta existe en cualquier estado, aunque no sea visible")
    void existe() {
        Oferta publicada = escenario.ofertaPublicada("Tour", "50000", ZONA_ACTIVA);
        Oferta retirada = escenario.ofertaPublicada("Retirada", "50000", ZONA_ACTIVA);
        escenario.gestion.retirar(new RetirarOfertaCommand(retirada.id(), PRESTADOR));
        Oferta enModeracion = escenario.publicar(AGENCIA, actividad("Sin aprobar", "50000", ZONA_ACTIVA));

        assertThat(servicio.existe(publicada.id())).isTrue();
        assertThat(servicio.existe(retirada.id())).isTrue();
        assertThat(servicio.existe(enModeracion.id())).isTrue();
        assertThat(servicio.existe("no-existe")).isFalse();
    }

    @Test
    @DisplayName("la visibilidad es la de la ficha: retirada, sin aprobar o con el RNT inactivo no es visible")
    void esVisible() {
        Oferta visible = escenario.ofertaPublicada("Tour", "50000", ZONA_ACTIVA);
        Oferta retirada = escenario.ofertaPublicada("Retirada", "50000", ZONA_ACTIVA);
        escenario.gestion.retirar(new RetirarOfertaCommand(retirada.id(), PRESTADOR));
        Oferta enModeracion = escenario.publicar(AGENCIA, actividad("Sin aprobar", "50000", ZONA_ACTIVA));

        assertThat(servicio.esVisible(visible.id())).isTrue();
        assertThat(servicio.esVisible(retirada.id())).isFalse();
        assertThat(servicio.esVisible(enModeracion.id())).isFalse();
        assertThat(servicio.esVisible("no-existe")).isFalse();

        escenario.habilitar(PRESTADOR, false, 2);
        assertThat(servicio.esVisible(visible.id())).isFalse();
    }

    @Test
    @DisplayName("un paquete deja de ser visible si retiran una de sus actividades (RF-22)")
    void paqueteConActividadRetirada() {
        Oferta caminata = escenario.ofertaPublicada("Caminata", "50000", ZONA_ACTIVA);
        Oferta kayak = escenario.ofertaPublicada("Kayak", "70000", ZONA_ACTIVA);
        Oferta paquete = escenario.publicar(AGENCIA, paquete(caminata.id(), kayak.id()));
        escenario.moderar(paquete.id(), 1, DecisionModeracion.APROBAR);
        assertThat(servicio.esVisible(paquete.id())).isTrue();

        escenario.gestion.retirar(new RetirarOfertaCommand(kayak.id(), PRESTADOR));

        assertThat(servicio.esVisible(paquete.id())).isFalse();
    }

    @Test
    @DisplayName("el nombre es el de la versión publicada, o el de la pendiente si aún no tiene publicada")
    void nombres() {
        Oferta publicada = escenario.ofertaPublicada("Tour al amanecer", "50000", ZONA_ACTIVA);
        escenario.gestion.editar(new EditarOfertaCommand(publicada.id(), AGENCIA,
                actividad("Tour al amanecer editado", "55000", ZONA_ACTIVA)));
        Oferta nueva = escenario.publicar(AGENCIA, actividad("Tour nuevo", "50000", ZONA_ACTIVA));

        Map<String, String> nombres = servicio.nombres(List.of(publicada.id(), nueva.id(), "no-existe",
                publicada.id()));

        assertThat(nombres).containsExactlyInAnyOrderEntriesOf(Map.of(
                publicada.id(), "Tour al amanecer",
                nueva.id(), "Tour nuevo"));
        assertThat(servicio.nombres(List.of())).isEmpty();
    }
}
