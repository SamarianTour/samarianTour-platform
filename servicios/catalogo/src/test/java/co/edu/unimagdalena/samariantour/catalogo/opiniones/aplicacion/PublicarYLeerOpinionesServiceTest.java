package co.edu.unimagdalena.samariantour.catalogo.opiniones.aplicacion;

import co.edu.unimagdalena.samariantour.catalogo.opiniones.dominio.excepcion.DatoInvalidoException;
import co.edu.unimagdalena.samariantour.catalogo.opiniones.dominio.excepcion.OfertaNoEncontradaException;
import co.edu.unimagdalena.samariantour.catalogo.opiniones.dominio.excepcion.OpinionDuplicadaException;
import co.edu.unimagdalena.samariantour.catalogo.opiniones.dominio.excepcion.OpinionNoHabilitadaException;
import co.edu.unimagdalena.samariantour.catalogo.opiniones.dominio.modelo.EstadoOpinion;
import co.edu.unimagdalena.samariantour.catalogo.opiniones.dominio.modelo.HabilitacionOpinion;
import co.edu.unimagdalena.samariantour.catalogo.opiniones.dominio.modelo.Opinion;
import co.edu.unimagdalena.samariantour.catalogo.opiniones.dominio.modelo.OpinionConOferta;
import co.edu.unimagdalena.samariantour.catalogo.opiniones.dominio.modelo.OpinionesDeOferta;
import co.edu.unimagdalena.samariantour.catalogo.opiniones.dominio.modelo.Paginacion;
import co.edu.unimagdalena.samariantour.catalogo.opiniones.dominio.puerto.entrada.comando.PublicarOpinionCommand;
import co.edu.unimagdalena.samariantour.catalogo.opiniones.infraestructura.salida.memoria.HabilitacionOpinionMemoriaAdapter;
import co.edu.unimagdalena.samariantour.catalogo.opiniones.infraestructura.salida.memoria.OpinionMemoriaAdapter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Publicar, leer y listar opiniones con los adaptadores en memoria (vacíos) y un doble del catálogo.
 */
class PublicarYLeerOpinionesServiceTest {

    private static final UUID LAURA = UUID.fromString("70000000-0000-0000-0000-000000000007");
    private static final UUID ANDRES = UUID.fromString("80000000-0000-0000-0000-000000000008");
    private static final String CAMINATA = "b00000000000000000000001";
    private static final String KAYAK = "b00000000000000000000004";
    private static final String RETIRADA = "b00000000000000000000099";
    // Con nanosegundos, para comprobar que la fecha se guarda en milisegundos, como en MongoDB.
    private static final Instant AHORA = Instant.parse("2026-10-21T15:00:00.123456789Z");
    private static final String COMENTARIO = "El guía conocía muy bien el sendero y cuidó el grupo.";

    private final OpinionMemoriaAdapter opiniones = new OpinionMemoriaAdapter(List.of());
    private final HabilitacionOpinionMemoriaAdapter habilitaciones = new HabilitacionOpinionMemoriaAdapter(List.of(
            new HabilitacionOpinion(LAURA, CAMINATA), new HabilitacionOpinion(ANDRES, CAMINATA),
            new HabilitacionOpinion(LAURA, RETIRADA)));
    private final OfertasDePrueba ofertas = new OfertasDePrueba()
            .visible(CAMINATA, "Caminata a Cabo San Juan")
            .visible(KAYAK, "Kayak en la bahía")
            .noVisible(RETIRADA, "Tour retirado");
    private final Clock reloj = Clock.fixed(AHORA, ZoneOffset.UTC);

    private final PublicarOpinionService publicar = new PublicarOpinionService(opiniones, habilitaciones, ofertas,
            reloj);
    private final LeerOpinionesOfertaService leer = new LeerOpinionesOfertaService(opiniones, ofertas);
    private final ListarOpinionesTuristaService listar = new ListarOpinionesTuristaService(opiniones, ofertas);

    private OpinionConOferta opinar(UUID turista, String oferta, int calificacion) {
        return publicar.publicar(new PublicarOpinionCommand(turista, oferta, calificacion, COMENTARIO));
    }

    @Nested
    @DisplayName("Publicar (RF-60, RF-61)")
    class Publicar {

        @Test
        @DisplayName("el turista que pagó la oferta deja su opinión: queda en moderación, con la fecha en milisegundos")
        void publicar() {
            OpinionConOferta publicada = opinar(LAURA, CAMINATA, 5);

            assertThat(publicada.nombreOferta()).isEqualTo("Caminata a Cabo San Juan");
            assertThat(publicada.opinion().estado()).isEqualTo(EstadoOpinion.EN_MODERACION);
            assertThat(publicada.opinion().fecha()).isEqualTo(Instant.parse("2026-10-21T15:00:00.123Z"));
            assertThat(opiniones.buscarPorId(publicada.opinion().id())).hasValueSatisfying(guardada ->
                    assertThat(guardada.turistaId()).isEqualTo(LAURA));
        }

        @Test
        @DisplayName("sin una solicitud pagada de la oferta no se puede opinar: 403 OPINION_NO_HABILITADA (DC-40)")
        void sinHabilitacion() {
            assertThatThrownBy(() -> opinar(ANDRES, KAYAK, 4))
                    .isInstanceOf(OpinionNoHabilitadaException.class)
                    .extracting("codigo").isEqualTo("OPINION_NO_HABILITADA");
        }

        @Test
        @DisplayName("una segunda opinión sobre la misma oferta se rechaza: 409 OPINION_DUPLICADA")
        void duplicada() {
            opinar(LAURA, CAMINATA, 5);

            assertThatThrownBy(() -> opinar(LAURA, CAMINATA, 2))
                    .isInstanceOf(OpinionDuplicadaException.class)
                    .extracting("codigo").isEqualTo("OPINION_DUPLICADA");
        }

        @Test
        @DisplayName("una oferta que no existe responde 404; una que existe pero ya no es visible sí se puede calificar")
        void ofertaInexistente() {
            assertThatThrownBy(() -> opinar(LAURA, "b000000000000000000000ff", 4))
                    .isInstanceOf(OfertaNoEncontradaException.class);

            assertThat(opinar(LAURA, RETIRADA, 3).opinion().estaEnModeracion()).isTrue();
        }

        @Test
        @DisplayName("los datos se validan antes que el resto de las reglas")
        void datosInvalidos() {
            assertThatThrownBy(() -> publicar.publicar(new PublicarOpinionCommand(ANDRES, KAYAK, 7, COMENTARIO)))
                    .isInstanceOf(DatoInvalidoException.class)
                    .hasMessage("calificacion: debe estar entre 1 y 5");
            assertThatThrownBy(() -> publicar.publicar(new PublicarOpinionCommand(ANDRES, KAYAK, 4, "Corto")))
                    .isInstanceOf(DatoInvalidoException.class);
        }
    }

    @Nested
    @DisplayName("Leer (RF-59)")
    class Leer {

        @Test
        @DisplayName("solo muestra las aprobadas, de la más reciente a la más antigua, con su resumen")
        void soloAprobadas() {
            Opinion deLaura = opinar(LAURA, CAMINATA, 5).opinion();
            Opinion deAndres = opinar(ANDRES, CAMINATA, 4).opinion();
            aprobar(deLaura.id(), AHORA.plusSeconds(60));

            OpinionesDeOferta antes = leer.leer(CAMINATA, Paginacion.de(null, null));
            assertThat(antes.pagina().contenido()).extracting(Opinion::id).containsExactly(deLaura.id());
            assertThat(antes.resumen().promedio()).contains(new BigDecimal("5.0"));

            aprobar(deAndres.id(), AHORA.plusSeconds(120));
            OpinionesDeOferta despues = leer.leer(CAMINATA, Paginacion.de(null, null));
            assertThat(despues.resumen().total()).isEqualTo(2);
            assertThat(despues.resumen().promedio()).contains(new BigDecimal("4.5"));
        }

        @Test
        @DisplayName("una oferta sin opiniones aprobadas tiene resumen vacío, sin promedio")
        void sinOpiniones() {
            OpinionesDeOferta kayak = leer.leer(KAYAK, Paginacion.de(null, null));

            assertThat(kayak.pagina().contenido()).isEmpty();
            assertThat(kayak.resumen().total()).isZero();
            assertThat(kayak.resumen().promedio()).isEmpty();
        }

        @Test
        @DisplayName("una oferta que no es visible no muestra sus opiniones: 404, igual que la ficha (DC-42)")
        void ofertaNoVisible() {
            assertThatThrownBy(() -> leer.leer(RETIRADA, Paginacion.de(null, null)))
                    .isInstanceOf(OfertaNoEncontradaException.class);
            assertThatThrownBy(() -> leer.leer("no-existe", Paginacion.de(null, null)))
                    .isInstanceOf(OfertaNoEncontradaException.class);
        }

        private void aprobar(String opinionId, Instant cuando) {
            Opinion opinion = opiniones.buscarPorId(opinionId).orElseThrow();
            opinion.aprobar(cuando);
            opiniones.guardar(opinion);
        }
    }

    @Nested
    @DisplayName("Mis opiniones")
    class MisOpiniones {

        @Test
        @DisplayName("el turista ve las suyas en cualquier estado, con el nombre de cada oferta y una sola consulta")
        void propias() {
            opinar(LAURA, CAMINATA, 5);
            opinar(LAURA, RETIRADA, 2);
            opinar(ANDRES, CAMINATA, 4);
            int antes = ofertas.consultasDeNombres();

            List<OpinionConOferta> deLaura = listar.listar(LAURA, Paginacion.de(null, null)).contenido();

            assertThat(deLaura).extracting(OpinionConOferta::nombreOferta)
                    .containsExactlyInAnyOrder("Caminata a Cabo San Juan", "Tour retirado");
            assertThat(ofertas.consultasDeNombres() - antes).isEqualTo(1);
            assertThat(listar.listar(UUID.randomUUID(), Paginacion.de(null, null)).contenido()).isEmpty();
        }
    }
}
