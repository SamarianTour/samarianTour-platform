package co.edu.unimagdalena.samariantour.catalogo.opiniones.aplicacion;

import co.edu.unimagdalena.samariantour.catalogo.compartido.concurrencia.ConflictoConcurrenciaException;
import co.edu.unimagdalena.samariantour.catalogo.compartido.concurrencia.EjecutorConReintento;
import co.edu.unimagdalena.samariantour.catalogo.opiniones.dominio.excepcion.OpinionNoEncontradaException;
import co.edu.unimagdalena.samariantour.catalogo.opiniones.dominio.modelo.EstadoOpinion;
import co.edu.unimagdalena.samariantour.catalogo.opiniones.dominio.modelo.Opinion;
import co.edu.unimagdalena.samariantour.catalogo.opiniones.dominio.modelo.OpinionConOferta;
import co.edu.unimagdalena.samariantour.catalogo.opiniones.dominio.modelo.Paginacion;
import co.edu.unimagdalena.samariantour.catalogo.opiniones.dominio.puerto.entrada.comando.AvisoHabilitacionOpinion;
import co.edu.unimagdalena.samariantour.catalogo.opiniones.dominio.puerto.entrada.comando.AvisoModeracionOpinion;
import co.edu.unimagdalena.samariantour.catalogo.opiniones.dominio.puerto.entrada.comando.AvisoSupresionTurista;
import co.edu.unimagdalena.samariantour.catalogo.opiniones.dominio.puerto.entrada.comando.DecisionModeracion;
import co.edu.unimagdalena.samariantour.catalogo.opiniones.dominio.puerto.entrada.comando.ResultadoAviso;
import co.edu.unimagdalena.samariantour.catalogo.opiniones.dominio.puerto.salida.AvisoOpinionProcesadoPort;
import co.edu.unimagdalena.samariantour.catalogo.opiniones.infraestructura.salida.demo.DatosOpiniones;
import co.edu.unimagdalena.samariantour.catalogo.opiniones.infraestructura.salida.memoria.AvisoOpinionProcesadoMemoriaAdapter;
import co.edu.unimagdalena.samariantour.catalogo.opiniones.infraestructura.salida.memoria.HabilitacionOpinionMemoriaAdapter;
import co.edu.unimagdalena.samariantour.catalogo.opiniones.infraestructura.salida.memoria.OpinionMemoriaAdapter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;

import static co.edu.unimagdalena.samariantour.catalogo.opiniones.infraestructura.salida.demo.DatosOpiniones.ANDRES;
import static co.edu.unimagdalena.samariantour.catalogo.opiniones.infraestructura.salida.demo.DatosOpiniones.CAMINATA_CABO_SAN_JUAN;
import static co.edu.unimagdalena.samariantour.catalogo.opiniones.infraestructura.salida.demo.DatosOpiniones.KAYAK_BAHIA_TAGANGA;
import static co.edu.unimagdalena.samariantour.catalogo.opiniones.infraestructura.salida.demo.DatosOpiniones.OPINION_ANDRES_KAYAK;
import static co.edu.unimagdalena.samariantour.catalogo.opiniones.infraestructura.salida.demo.DatosOpiniones.OPINION_EN_MODERACION_CAFE;
import static co.edu.unimagdalena.samariantour.catalogo.opiniones.infraestructura.salida.demo.DatosOpiniones.OPINION_LAURA_CABO_SAN_JUAN;
import static co.edu.unimagdalena.samariantour.catalogo.opiniones.infraestructura.salida.demo.DatosOpiniones.OPINION_RECHAZADA;
import static co.edu.unimagdalena.samariantour.catalogo.opiniones.infraestructura.salida.demo.DatosOpiniones.RECORRIDO_CENTRO_HISTORICO;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Los avisos del núcleo sobre opiniones y la cola del administrador, con los adaptadores en memoria cargados con los
 * datos de demostración.
 */
class ModerarYSuprimirOpinionesServiceTest {

    private static final Instant AHORA = Instant.parse("2026-10-22T10:00:00Z");

    private final OpinionMemoriaAdapter opiniones = new OpinionMemoriaAdapter();
    private final HabilitacionOpinionMemoriaAdapter habilitaciones = new HabilitacionOpinionMemoriaAdapter();
    private final AvisoOpinionProcesadoMemoriaAdapter procesados = new AvisoOpinionProcesadoMemoriaAdapter();
    private final OfertasDePrueba ofertas = new OfertasDePrueba()
            .visible(CAMINATA_CABO_SAN_JUAN, "Caminata a Cabo San Juan del Guía")
            .visible(KAYAK_BAHIA_TAGANGA, "Kayak en la bahía de Taganga")
            .visible(RECORRIDO_CENTRO_HISTORICO, "Recorrido a pie por el Centro Histórico")
            .visible(DatosOpiniones.RUTA_CAFE_CACAO_MINCA, "Ruta del café y el cacao");
    /** Los mismos 3 intentos que en producción, sin esperar entre ellos. */
    private final EjecutorConReintento reintento = new EjecutorConReintento(3, Duration.ZERO);

    private final AplicarAvisoOpinionService avisos = servicio(procesados);
    private final ListarOpinionesPendientesService pendientes = new ListarOpinionesPendientesService(opiniones,
            ofertas);

    private int secuencia;

    private AplicarAvisoOpinionService servicio(AvisoOpinionProcesadoPort registro) {
        return new AplicarAvisoOpinionService(opiniones, habilitaciones, ofertas, registro,
                Clock.fixed(AHORA, ZoneOffset.UTC), reintento);
    }

    private String idAviso() {
        return "aviso-" + ++secuencia;
    }

    @Nested
    @DisplayName("Habilitación (DC-40)")
    class Habilitacion {

        @Test
        @DisplayName("una solicitud pagada habilita al turista; el mismo aviso repetido es DUPLICADO")
        void habilitar() {
            AvisoHabilitacionOpinion aviso = new AvisoHabilitacionOpinion("pago-1", ANDRES, RECORRIDO_CENTRO_HISTORICO);

            assertThat(avisos.aplicar(aviso)).isEqualTo(ResultadoAviso.APLICADO);
            assertThat(avisos.aplicar(aviso)).isEqualTo(ResultadoAviso.DUPLICADO);
            assertThat(habilitaciones.existe(ANDRES, RECORRIDO_CENTRO_HISTORICO)).isTrue();
        }

        @Test
        @DisplayName("otro aviso de una habilitación que ya existe, o de una oferta que no existe, se ignora")
        void ignorados() {
            assertThat(avisos.aplicar(new AvisoHabilitacionOpinion(idAviso(), ANDRES, KAYAK_BAHIA_TAGANGA)))
                    .isEqualTo(ResultadoAviso.IGNORADO);
            assertThat(avisos.aplicar(new AvisoHabilitacionOpinion(idAviso(), ANDRES, "b000000000000000000000ff")))
                    .isEqualTo(ResultadoAviso.IGNORADO);
            assertThat(habilitaciones.existe(ANDRES, "b000000000000000000000ff")).isFalse();
        }
    }

    @Nested
    @DisplayName("Moderación (DC-43)")
    class Moderacion {

        @Test
        @DisplayName("aprobar publica la opinión; la decisión repetida con otro id se ignora (DC-08)")
        void aprobar() {
            assertThat(avisos.aplicar(new AvisoModeracionOpinion(idAviso(), OPINION_ANDRES_KAYAK,
                    DecisionModeracion.APROBAR, null))).isEqualTo(ResultadoAviso.APLICADO);
            assertThat(avisos.aplicar(new AvisoModeracionOpinion(idAviso(), OPINION_ANDRES_KAYAK,
                    DecisionModeracion.RECHAZAR, "Tarde"))).isEqualTo(ResultadoAviso.IGNORADO);

            Opinion aprobada = opiniones.buscarPorId(OPINION_ANDRES_KAYAK).orElseThrow();
            assertThat(aprobada.estaAprobada()).isTrue();
            assertThat(aprobada.revisadaEn()).isEqualTo(AHORA);
        }

        @Test
        @DisplayName("rechazar guarda el motivo para el turista")
        void rechazar() {
            assertThat(avisos.aplicar(new AvisoModeracionOpinion(idAviso(), OPINION_EN_MODERACION_CAFE,
                    DecisionModeracion.RECHAZAR, "Incluye datos personales"))).isEqualTo(ResultadoAviso.APLICADO);

            Opinion rechazada = opiniones.buscarPorId(OPINION_EN_MODERACION_CAFE).orElseThrow();
            assertThat(rechazada.estado()).isEqualTo(EstadoOpinion.RECHAZADA);
            assertThat(rechazada.motivoRechazo()).isEqualTo("Incluye datos personales");
        }

        @Test
        @DisplayName("una opinión que no existe o que ya se moderó ignora el aviso")
        void ignorados() {
            assertThat(avisos.aplicar(new AvisoModeracionOpinion(idAviso(), "d000000000000000000000ff",
                    DecisionModeracion.APROBAR, null))).isEqualTo(ResultadoAviso.IGNORADO);
            assertThat(avisos.aplicar(new AvisoModeracionOpinion(idAviso(), OPINION_RECHAZADA,
                    DecisionModeracion.APROBAR, null))).isEqualTo(ResultadoAviso.IGNORADO);
            assertThat(opiniones.buscarPorId(OPINION_RECHAZADA).orElseThrow().estado())
                    .isEqualTo(EstadoOpinion.RECHAZADA);
        }

        @Test
        @DisplayName("si otra escritura se cruza siempre, el aviso falla sin quedar procesado y el núcleo lo reenvía")
        void conflictoSinMarcar() {
            AvisoOpinionProcesadoPort registro = new AvisoOpinionProcesadoMemoriaAdapter();
            OpinionMemoriaAdapter conflictiva = spy(new OpinionMemoriaAdapter());
            doThrow(new ConflictoConcurrenciaException("Otra réplica la cambió")).when(conflictiva).guardar(any());
            AplicarAvisoOpinionService servicio = new AplicarAvisoOpinionService(conflictiva, habilitaciones,
                    ofertas, registro, Clock.fixed(AHORA, ZoneOffset.UTC), reintento);

            assertThatThrownBy(() -> servicio.aplicar(new AvisoModeracionOpinion("conflicto-1",
                    OPINION_ANDRES_KAYAK, DecisionModeracion.APROBAR, null)))
                    .isInstanceOf(ConflictoConcurrenciaException.class);
            assertThat(registro.fueProcesado("conflicto-1")).isFalse();
            verify(conflictiva, times(3)).guardar(any());
        }

        @Test
        @DisplayName("el administrador ve la cola de la más antigua a la más reciente, con el nombre de la oferta")
        void cola() {
            assertThat(pendientes.listar(Paginacion.de(null, null)).contenido())
                    .extracting(conOferta -> conOferta.opinion().id(), OpinionConOferta::nombreOferta)
                    .containsExactly(
                            org.assertj.core.groups.Tuple.tuple(OPINION_EN_MODERACION_CAFE, "Ruta del café y el cacao"),
                            org.assertj.core.groups.Tuple.tuple(OPINION_ANDRES_KAYAK, "Kayak en la bahía de Taganga"));
        }

        @Test
        @DisplayName("el núcleo consulta una opinión en moderación; una moderada o inexistente da 404")
        void consultaDelNucleo() {
            assertThat(pendientes.consultar(OPINION_ANDRES_KAYAK).turistaId()).isEqualTo(ANDRES);
            for (String id : new String[] {OPINION_LAURA_CABO_SAN_JUAN, OPINION_RECHAZADA, "no-existe"}) {
                assertThatThrownBy(() -> pendientes.consultar(id)).isInstanceOf(OpinionNoEncontradaException.class);
            }
        }
    }

    @Nested
    @DisplayName("Supresión (DC-44)")
    class Supresion {

        @Test
        @DisplayName("borra las opiniones y las habilitaciones del turista, sin tocar las de los demás")
        void suprimir() {
            assertThat(avisos.aplicar(new AvisoSupresionTurista(idAviso(), ANDRES))).isEqualTo(ResultadoAviso.APLICADO);

            assertThat(opiniones.buscarDelTurista(ANDRES, Paginacion.de(null, null)).totalElementos()).isZero();
            assertThat(habilitaciones.existe(ANDRES, KAYAK_BAHIA_TAGANGA)).isFalse();
            assertThat(opiniones.buscarPorId(OPINION_LAURA_CABO_SAN_JUAN)).isPresent();
            assertThat(pendientes.listar(Paginacion.de(null, null)).contenido())
                    .extracting(conOferta -> conOferta.opinion().id()).containsExactly(OPINION_EN_MODERACION_CAFE);
        }

        @Test
        @DisplayName("una moderación que llega después de la supresión se ignora")
        void moderacionDespuesDeSuprimir() {
            avisos.aplicar(new AvisoSupresionTurista(idAviso(), ANDRES));

            assertThat(avisos.aplicar(new AvisoModeracionOpinion(idAviso(), OPINION_ANDRES_KAYAK,
                    DecisionModeracion.APROBAR, null))).isEqualTo(ResultadoAviso.IGNORADO);
        }

        @Test
        @DisplayName("suprimir a un turista sin opiniones ni habilitaciones no cambia nada: se ignora")
        void sinNadaQueBorrar() {
            assertThat(avisos.aplicar(new AvisoSupresionTurista(idAviso(), UUID.randomUUID())))
                    .isEqualTo(ResultadoAviso.IGNORADO);
        }
    }
}
