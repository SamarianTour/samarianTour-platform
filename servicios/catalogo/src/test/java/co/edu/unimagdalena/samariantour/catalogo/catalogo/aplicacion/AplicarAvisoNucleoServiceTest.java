package co.edu.unimagdalena.samariantour.catalogo.catalogo.aplicacion;

import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.EstadoModeracion;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.Oferta;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.PrestadorCatalogo;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.puerto.entrada.comando.AvisoHabilitacion;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.puerto.entrada.comando.AvisoModeracion;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.puerto.entrada.comando.DecisionModeracion;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.puerto.entrada.comando.EditarOfertaCommand;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.puerto.entrada.comando.ResultadoAviso;
import co.edu.unimagdalena.samariantour.catalogo.compartido.concurrencia.ConflictoConcurrenciaException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static co.edu.unimagdalena.samariantour.catalogo.catalogo.aplicacion.EscenarioAplicacion.AGENCIA;
import static co.edu.unimagdalena.samariantour.catalogo.catalogo.aplicacion.EscenarioAplicacion.actividad;
import static co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.DatosDePrueba.PRESTADOR;
import static co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.DatosDePrueba.ZONA_ACTIVA;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AplicarAvisoNucleoServiceTest {

    private final EscenarioAplicacion escenario = new EscenarioAplicacion();
    private final AplicarAvisoNucleoService servicio = escenario.avisos;

    @Test
    @DisplayName("un aviso repetido se reconoce y no se vuelve a aplicar")
    void duplicado() {
        AvisoHabilitacion aviso = new AvisoHabilitacion("a-1", PRESTADOR, true, 1);

        assertThat(servicio.aplicar(aviso)).isEqualTo(ResultadoAviso.APLICADO);
        assertThat(servicio.aplicar(aviso)).isEqualTo(ResultadoAviso.DUPLICADO);
    }

    @Nested
    @DisplayName("Habilitación RNT")
    class Habilitacion {

        @Test
        @DisplayName("un aviso viejo, que llega en desorden, se ignora")
        void avisoViejo() {
            escenario.habilitar(PRESTADOR, false, 5);

            ResultadoAviso resultado = escenario.habilitar(PRESTADOR, true, 4);

            assertThat(resultado).isEqualTo(ResultadoAviso.IGNORADO);
            assertThat(escenario.prestadores.buscarPorId(PRESTADOR).orElseThrow().rntActivo()).isFalse();
        }

        @Test
        @DisplayName("al perder el RNT, las ofertas del prestador dejan de ser visibles; al restituirlo, vuelven")
        void retiroYRestitucion() {
            Oferta oferta = escenario.ofertaPublicada("Tour", "50000", ZONA_ACTIVA);

            escenario.habilitar(PRESTADOR, false, 2);
            assertThat(escenario.guardada(oferta.id()).estaVisible()).isFalse();

            escenario.habilitar(PRESTADOR, true, 3);
            assertThat(escenario.guardada(oferta.id()).estaVisible()).isTrue();
        }

        @Test
        @DisplayName("si el proceso cayó después de actualizar la réplica, reintentar el aviso actualiza las ofertas")
        void recuperacionTrasCaida() {
            Oferta oferta = escenario.ofertaPublicada("Tour", "50000", ZONA_ACTIVA);
            // Simula la caída: la réplica ya tiene la versión 2, pero las ofertas y el registro del aviso no.
            escenario.prestadores.guardar(new PrestadorCatalogo(PRESTADOR, false, 2));

            servicio.aplicar(new AvisoHabilitacion("reintento", PRESTADOR, false, 2));

            assertThat(escenario.guardada(oferta.id()).estaVisible()).isFalse();
        }
    }

    @Nested
    @DisplayName("Moderación")
    class Moderacion {

        @Test
        @DisplayName("aprobar con el RNT inactivo deja la oferta publicada pero no visible")
        void aprobarSinRnt() {
            escenario.habilitar(PRESTADOR, true, 1);
            Oferta oferta = escenario.publicar(AGENCIA, actividad("Tour", "50000", ZONA_ACTIVA));
            escenario.habilitar(PRESTADOR, false, 2);

            escenario.moderar(oferta.id(), 1, DecisionModeracion.APROBAR);

            Oferta guardada = escenario.guardada(oferta.id());
            assertThat(guardada.publicada()).isPresent();
            assertThat(guardada.estaVisible()).isFalse();
        }

        @Test
        @DisplayName("rechazar deja la versión rechazada con su motivo")
        void rechazar() {
            escenario.habilitar(PRESTADOR, true, 1);
            Oferta oferta = escenario.publicar(AGENCIA, actividad("Tour", "50000", ZONA_ACTIVA));

            ResultadoAviso resultado = servicio.aplicar(new AvisoModeracion(escenario.siguienteIdAviso(),
                    oferta.id(), 1, DecisionModeracion.RECHAZAR, "Descripción incompleta"));

            assertThat(resultado).isEqualTo(ResultadoAviso.APLICADO);
            assertThat(escenario.guardada(oferta.id()).pendiente()).hasValueSatisfying(version -> {
                assertThat(version.estadoModeracion()).isEqualTo(EstadoModeracion.RECHAZADA);
                assertThat(version.moderacion().motivoRechazo()).isEqualTo("Descripción incompleta");
            });
        }

        @Test
        @DisplayName("la moderación de una versión reemplazada se ignora, pero el aviso queda registrado")
        void versionReemplazada() {
            escenario.habilitar(PRESTADOR, true, 1);
            Oferta oferta = escenario.publicar(AGENCIA, actividad("Tour", "50000", ZONA_ACTIVA));
            escenario.gestion.editar(new EditarOfertaCommand(oferta.id(), AGENCIA,
                    actividad("Tour v2", "50000", ZONA_ACTIVA)));
            AvisoModeracion avisoViejo = new AvisoModeracion("mod-v1", oferta.id(), 1, DecisionModeracion.APROBAR,
                    null);

            assertThat(servicio.aplicar(avisoViejo)).isEqualTo(ResultadoAviso.IGNORADO);
            assertThat(servicio.aplicar(avisoViejo)).isEqualTo(ResultadoAviso.DUPLICADO);
            assertThat(escenario.guardada(oferta.id()).publicada()).isEmpty();
        }

        @Test
        @DisplayName("la moderación de una oferta inexistente se ignora")
        void ofertaInexistente() {
            assertThat(escenario.moderar("no-existe", 1, DecisionModeracion.APROBAR))
                    .isEqualTo(ResultadoAviso.IGNORADO);
        }
    }

    @Nested
    @DisplayName("Concurrencia (ADR-05)")
    class Concurrencia {

        @Test
        @DisplayName("si otra escritura se cruza con la aprobación, el aviso se reintenta y se aplica una sola vez")
        void moderacionSeReintenta() {
            escenario.habilitar(PRESTADOR, true, 1);
            Oferta oferta = escenario.publicar(AGENCIA, actividad("Tour", "50000", ZONA_ACTIVA));
            escenario.ofertas.simularConflictos(1);
            AvisoModeracion aviso = new AvisoModeracion("mod-1", oferta.id(), 1, DecisionModeracion.APROBAR, null);

            assertThat(servicio.aplicar(aviso)).isEqualTo(ResultadoAviso.APLICADO);
            assertThat(escenario.guardada(oferta.id()).publicada()).isPresent();
            assertThat(servicio.aplicar(aviso)).isEqualTo(ResultadoAviso.DUPLICADO);
        }

        @Test
        @DisplayName("si se agotan los intentos, el aviso no queda procesado y el núcleo puede reenviarlo")
        void avisoAgotaLosIntentos() {
            escenario.habilitar(PRESTADOR, true, 1);
            Oferta oferta = escenario.publicar(AGENCIA, actividad("Tour", "50000", ZONA_ACTIVA));
            escenario.ofertas.simularConflictos(3);
            AvisoModeracion aviso = new AvisoModeracion("mod-1", oferta.id(), 1, DecisionModeracion.APROBAR, null);

            assertThatThrownBy(() -> servicio.aplicar(aviso)).isInstanceOf(ConflictoConcurrenciaException.class);
            assertThat(escenario.avisosProcesados.fueProcesado("mod-1")).isFalse();

            assertThat(servicio.aplicar(aviso)).isEqualTo(ResultadoAviso.APLICADO);
        }

        @Test
        @DisplayName("dos avisos de habilitación en orden inverso dejan la réplica y las ofertas en la versión más nueva")
        void habilitacionEnDesorden() {
            Oferta oferta = escenario.ofertaPublicada("Tour", "50000", ZONA_ACTIVA);

            escenario.habilitar(PRESTADOR, false, 3);
            ResultadoAviso atrasado = escenario.habilitar(PRESTADOR, true, 2);

            assertThat(atrasado).isEqualTo(ResultadoAviso.IGNORADO);
            PrestadorCatalogo replica = escenario.prestadores.buscarPorId(PRESTADOR).orElseThrow();
            assertThat(replica.rntActivo()).isFalse();
            assertThat(replica.version()).isEqualTo(3);
            Oferta guardada = escenario.guardada(oferta.id());
            assertThat(guardada.rntPrestadorActivo()).isFalse();
            assertThat(guardada.versionHabilitacion()).isEqualTo(3);
        }

        @Test
        @DisplayName("una actualización masiva atrasada no cambia ofertas que ya tienen una versión más nueva")
        void actualizacionMasivaAtrasada() {
            Oferta oferta = escenario.ofertaPublicada("Tour", "50000", ZONA_ACTIVA);
            escenario.habilitar(PRESTADOR, false, 3);

            escenario.ofertas.actualizarRntPrestador(PRESTADOR, true, 2);

            assertThat(escenario.guardada(oferta.id()).rntPrestadorActivo()).isFalse();
        }
    }
}
