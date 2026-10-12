package co.edu.unimagdalena.samariantour.catalogo.catalogo.aplicacion;

import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.excepcion.OfertaNoEncontradaException;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.excepcion.PrestadorNoAutorizadoException;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.excepcion.RntInactivoException;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.excepcion.TipoOfertaNoPermitidoException;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.excepcion.ZonaNoDisponibleException;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.Oferta;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.Pagina;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.Paginacion;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.puerto.entrada.comando.EditarOfertaCommand;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.puerto.entrada.comando.RetirarOfertaCommand;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static co.edu.unimagdalena.samariantour.catalogo.catalogo.aplicacion.EscenarioAplicacion.AGENCIA;
import static co.edu.unimagdalena.samariantour.catalogo.catalogo.aplicacion.EscenarioAplicacion.OTRA_AGENCIA;
import static co.edu.unimagdalena.samariantour.catalogo.catalogo.aplicacion.EscenarioAplicacion.actividad;
import static co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.DatosDePrueba.AHORA;
import static co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.DatosDePrueba.OTRO_PRESTADOR;
import static co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.DatosDePrueba.PRESTADOR;
import static co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.DatosDePrueba.ZONA_ACTIVA;
import static co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.DatosDePrueba.ZONA_INACTIVA;
import static co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.DatosDePrueba.alojamiento;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GestionarOfertaServiceTest {

    private final EscenarioAplicacion escenario = new EscenarioAplicacion();
    private final GestionarOfertaService servicio = escenario.gestion;

    @Nested
    @DisplayName("Publicar")
    class Publicar {

        @Test
        @DisplayName("guarda la oferta con un id nuevo y su versión 1 en moderación")
        void guarda() {
            escenario.habilitar(PRESTADOR, true, 1);

            Oferta oferta = escenario.publicar(AGENCIA, actividad("Tour", "50000", ZONA_ACTIVA));

            assertThat(oferta.id()).isNotBlank();
            assertThat(escenario.ofertas.vecesGuardada(oferta.id())).isEqualTo(1);
            assertThat(oferta.pendiente().orElseThrow().moderacion().enviadaEn()).isEqualTo(AHORA);
        }

        @Test
        @DisplayName("un prestador del que no ha llegado aviso de habilitación no puede publicar")
        void sinAvisoDeHabilitacion() {
            assertThatThrownBy(() -> escenario.publicar(AGENCIA, actividad("Tour", "50000", ZONA_ACTIVA)))
                    .isInstanceOf(RntInactivoException.class);
            assertThat(escenario.ofertas.total()).isZero();
        }

        @Test
        @DisplayName("la zona debe existir")
        void zonaInexistente() {
            escenario.habilitar(PRESTADOR, true, 1);

            assertThatThrownBy(() -> escenario.publicar(AGENCIA, actividad("Tour", "50000", "no-existe")))
                    .isInstanceOf(ZonaNoDisponibleException.class);
        }

        @Test
        @DisplayName("la zona debe estar activa")
        void zonaDebeEstarActiva() {
            escenario.habilitar(PRESTADOR, true, 1);

            assertThatThrownBy(() -> escenario.publicar(AGENCIA, actividad("Tour", "50000", ZONA_INACTIVA)))
                    .isInstanceOf(ZonaNoDisponibleException.class);
        }

        @Test
        @DisplayName("el tipo de prestador del token decide qué puede publicar")
        void tipoDePrestador() {
            escenario.habilitar(PRESTADOR, true, 1);

            assertThatThrownBy(() -> escenario.publicar(AGENCIA, alojamiento()))
                    .isInstanceOf(TipoOfertaNoPermitidoException.class);
        }
    }

    @Nested
    @DisplayName("Editar y retirar")
    class EditarYRetirar {

        @Test
        @DisplayName("editar guarda la nueva versión pendiente")
        void editarGuarda() {
            Oferta oferta = escenario.ofertaPublicada("Tour", "50000", ZONA_ACTIVA);
            int guardadasAntes = escenario.ofertas.vecesGuardada(oferta.id());

            Oferta editada = servicio.editar(new EditarOfertaCommand(oferta.id(), AGENCIA,
                    actividad("Tour editado", "55000", ZONA_ACTIVA)));

            assertThat(editada.pendiente().orElseThrow().numero()).isEqualTo(2);
            assertThat(escenario.ofertas.vecesGuardada(oferta.id())).isEqualTo(guardadasAntes + 1);
        }

        @Test
        @DisplayName("editar una oferta que no existe responde no encontrada")
        void editarInexistente() {
            assertThatThrownBy(() -> servicio.editar(new EditarOfertaCommand("no-existe", AGENCIA,
                    actividad("Tour", "50000", ZONA_ACTIVA))))
                    .isInstanceOf(OfertaNoEncontradaException.class);
        }

        @Test
        @DisplayName("otro prestador no puede editar ni retirar la oferta")
        void otroPrestador() {
            Oferta oferta = escenario.ofertaPublicada("Tour", "50000", ZONA_ACTIVA);
            escenario.habilitar(OTRO_PRESTADOR, true, 1);

            assertThatThrownBy(() -> servicio.editar(new EditarOfertaCommand(oferta.id(), OTRA_AGENCIA,
                    actividad("Tour", "50000", ZONA_ACTIVA))))
                    .isInstanceOf(PrestadorNoAutorizadoException.class);
            assertThatThrownBy(() -> servicio.retirar(new RetirarOfertaCommand(oferta.id(), OTRO_PRESTADOR)))
                    .isInstanceOf(PrestadorNoAutorizadoException.class);
        }

        @Test
        @DisplayName("retirar guarda la oferta como retirada, sin borrarla")
        void retirarGuarda() {
            Oferta oferta = escenario.ofertaPublicada("Tour", "50000", ZONA_ACTIVA);

            servicio.retirar(new RetirarOfertaCommand(oferta.id(), PRESTADOR));

            assertThat(escenario.guardada(oferta.id()).retirada()).isTrue();
        }

        @Test
        @DisplayName("el prestador ve sus ofertas en cualquier estado, y no las de otros")
        void listarMias() {
            Oferta publicada = escenario.ofertaPublicada("Publicada", "50000", ZONA_ACTIVA);
            Oferta enModeracion = escenario.publicar(AGENCIA, actividad("En moderación", "60000", ZONA_ACTIVA));
            escenario.habilitar(OTRO_PRESTADOR, true, 1);
            escenario.publicar(OTRA_AGENCIA, actividad("De otro", "70000", ZONA_ACTIVA));

            Pagina<Oferta> mias = servicio.listarMias(PRESTADOR, Paginacion.de(null, null));

            assertThat(mias.contenido()).extracting(Oferta::id)
                    .containsExactlyInAnyOrder(publicada.id(), enModeracion.id());
        }
    }
}
