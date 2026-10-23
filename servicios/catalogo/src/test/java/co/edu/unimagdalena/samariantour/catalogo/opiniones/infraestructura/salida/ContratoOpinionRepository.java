package co.edu.unimagdalena.samariantour.catalogo.opiniones.infraestructura.salida;

import co.edu.unimagdalena.samariantour.catalogo.compartido.concurrencia.ConflictoConcurrenciaException;
import co.edu.unimagdalena.samariantour.catalogo.opiniones.dominio.excepcion.OpinionDuplicadaException;
import co.edu.unimagdalena.samariantour.catalogo.opiniones.dominio.modelo.EstadoOpinion;
import co.edu.unimagdalena.samariantour.catalogo.opiniones.dominio.modelo.HabilitacionOpinion;
import co.edu.unimagdalena.samariantour.catalogo.opiniones.dominio.modelo.Opinion;
import co.edu.unimagdalena.samariantour.catalogo.opiniones.dominio.modelo.Pagina;
import co.edu.unimagdalena.samariantour.catalogo.opiniones.dominio.modelo.Paginacion;
import co.edu.unimagdalena.samariantour.catalogo.opiniones.dominio.modelo.ResumenCalificaciones;
import co.edu.unimagdalena.samariantour.catalogo.opiniones.dominio.puerto.salida.HabilitacionOpinionRepositoryPort;
import co.edu.unimagdalena.samariantour.catalogo.opiniones.dominio.puerto.salida.OpinionRepositoryPort;
import co.edu.unimagdalena.samariantour.catalogo.opiniones.infraestructura.salida.demo.DatosOpiniones;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static co.edu.unimagdalena.samariantour.catalogo.opiniones.infraestructura.salida.demo.DatosOpiniones.ANDRES;
import static co.edu.unimagdalena.samariantour.catalogo.opiniones.infraestructura.salida.demo.DatosOpiniones.AVISTAMIENTO_AVES_MINCA;
import static co.edu.unimagdalena.samariantour.catalogo.opiniones.infraestructura.salida.demo.DatosOpiniones.CAMINATA_CABO_SAN_JUAN;
import static co.edu.unimagdalena.samariantour.catalogo.opiniones.infraestructura.salida.demo.DatosOpiniones.KAYAK_BAHIA_TAGANGA;
import static co.edu.unimagdalena.samariantour.catalogo.opiniones.infraestructura.salida.demo.DatosOpiniones.LAURA;
import static co.edu.unimagdalena.samariantour.catalogo.opiniones.infraestructura.salida.demo.DatosOpiniones.OPINION_ANDRES_KAYAK;
import static co.edu.unimagdalena.samariantour.catalogo.opiniones.infraestructura.salida.demo.DatosOpiniones.OPINION_EN_MODERACION_CAFE;
import static co.edu.unimagdalena.samariantour.catalogo.opiniones.infraestructura.salida.demo.DatosOpiniones.OPINION_LAURA_CABO_SAN_JUAN;
import static co.edu.unimagdalena.samariantour.catalogo.opiniones.infraestructura.salida.demo.DatosOpiniones.OPINION_RECHAZADA;
import static co.edu.unimagdalena.samariantour.catalogo.opiniones.infraestructura.salida.demo.DatosOpiniones.PAQUETE_TAYRONA_MAR_Y_SELVA;
import static co.edu.unimagdalena.samariantour.catalogo.opiniones.infraestructura.salida.demo.DatosOpiniones.RECORRIDO_CENTRO_HISTORICO;
import static co.edu.unimagdalena.samariantour.catalogo.opiniones.infraestructura.salida.demo.DatosOpiniones.SNORKEL_PLAYA_CRISTAL;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Contrato de los repositorios de opiniones y de habilitaciones, común a los adaptadores en memoria y de MongoDB: los
 * dos deben comportarse igual con los datos de demostración. Cada prueba recibe repositorios que solo tienen esos
 * datos.
 */
public interface ContratoOpinionRepository {

    Instant ENVIADA = Instant.parse("2026-10-21T15:00:00Z");

    /** Cargado con DatosOpiniones.opiniones() y nada más. */
    OpinionRepositoryPort opiniones();

    /** Cargado con DatosOpiniones.habilitaciones() y nada más. */
    HabilitacionOpinionRepositoryPort habilitaciones();

    private Opinion nueva(UUID turistaId, String ofertaId) {
        return Opinion.enviar(opiniones().nuevoId(), ofertaId, turistaId, 4,
                "Muy buena experiencia, la recomiendo a otros viajeros.", ENVIADA);
    }

    @Test
    @DisplayName("genera identificadores nuevos con formato ObjectId (DC-30)")
    default void nuevoId() {
        assertThat(ObjectId.isValid(opiniones().nuevoId())).isTrue();
        assertThat(opiniones().nuevoId()).isNotEqualTo(opiniones().nuevoId());
    }

    @Test
    @DisplayName("devuelve cada opinión como se guardó, con su estado, su motivo y sus fechas")
    default void idaYVuelta() {
        for (Opinion original : DatosOpiniones.opiniones()) {
            assertThat(opiniones().buscarPorId(original.id())).get()
                    .usingRecursiveComparison()
                    .ignoringFields("revision")
                    .isEqualTo(original);
        }
        assertThat(opiniones().buscarPorId(OPINION_RECHAZADA)).hasValueSatisfying(rechazada -> {
            assertThat(rechazada.estado()).isEqualTo(EstadoOpinion.RECHAZADA);
            assertThat(rechazada.motivoRechazo()).isEqualTo("Incluye publicidad y datos de contacto");
        });
    }

    @Test
    @DisplayName("un id que no existe o que no tiene formato ObjectId se trata como inexistente (DC-30)")
    default void idInexistente() {
        assertThat(opiniones().buscarPorId(new ObjectId().toHexString())).isEmpty();
        assertThat(opiniones().buscarPorId("no-es-un-objectid")).isEmpty();
        assertThat(opiniones().buscarAprobadas("no-es-un-objectid", Paginacion.de(null, null)).contenido()).isEmpty();
        assertThat(opiniones().resumirAprobadas("no-es-un-objectid")).isEqualTo(ResumenCalificaciones.SIN_OPINIONES);
    }

    @Test
    @DisplayName("guarda una opinión nueva con la primera revisión")
    default void guardarNueva() {
        Opinion opinion = nueva(LAURA, SNORKEL_PLAYA_CRISTAL);

        opiniones().guardar(opinion);

        assertThat(opiniones().buscarPorId(opinion.id())).hasValueSatisfying(guardada -> {
            assertThat(guardada).usingRecursiveComparison().ignoringFields("revision").isEqualTo(opinion);
            assertThat(guardada.revision()).isEqualTo(Opinion.REVISION_NUEVA + 1);
        });
        assertThat(opiniones().existeDe(LAURA, SNORKEL_PLAYA_CRISTAL)).isTrue();
    }

    @Test
    @DisplayName("una segunda opinión del mismo turista sobre la misma oferta se rechaza (RF-61)")
    default void opinionDuplicada() {
        Opinion segunda = nueva(LAURA, CAMINATA_CABO_SAN_JUAN);

        assertThatThrownBy(() -> opiniones().guardar(segunda)).isInstanceOf(OpinionDuplicadaException.class);
        assertThat(opiniones().buscarPorId(segunda.id())).isEmpty();
        assertThat(opiniones().existeDe(LAURA, CAMINATA_CABO_SAN_JUAN)).isTrue();
        assertThat(opiniones().existeDe(ANDRES, CAMINATA_CABO_SAN_JUAN)).isFalse();
    }

    @Test
    @DisplayName("guarda una opinión moderada y sube la revisión; con una revisión vieja hay conflicto (ADR-05)")
    default void bloqueoOptimista() {
        Opinion leida = opiniones().buscarPorId(OPINION_ANDRES_KAYAK).orElseThrow();
        Opinion otraLectura = opiniones().buscarPorId(OPINION_ANDRES_KAYAK).orElseThrow();
        leida.aprobar(ENVIADA);

        opiniones().guardar(leida);
        otraLectura.rechazar("Tarde", ENVIADA);

        assertThat(opiniones().buscarPorId(OPINION_ANDRES_KAYAK)).hasValueSatisfying(guardada -> {
            assertThat(guardada.estaAprobada()).isTrue();
            assertThat(guardada.revisadaEn()).isEqualTo(ENVIADA);
            assertThat(guardada.revision()).isEqualTo(leida.revision() + 1);
        });
        assertThatThrownBy(() -> opiniones().guardar(otraLectura))
                .isInstanceOf(ConflictoConcurrenciaException.class);
    }

    @Test
    @DisplayName("lee las aprobadas de una oferta de la más reciente a la más antigua, sin las demás (RF-59)")
    default void aprobadasDeUnaOferta() {
        Pagina<Opinion> pagina = opiniones().buscarAprobadas(CAMINATA_CABO_SAN_JUAN, new Paginacion(0, 2));

        assertThat(pagina.totalElementos()).as("la rechazada no cuenta").isEqualTo(3);
        assertThat(pagina.contenido()).extracting(Opinion::id)
                .containsExactly("d00000000000000000000003", "d00000000000000000000002");
        assertThat(opiniones().buscarAprobadas(CAMINATA_CABO_SAN_JUAN, new Paginacion(1, 2)).contenido())
                .extracting(Opinion::id).containsExactly(OPINION_LAURA_CABO_SAN_JUAN);
        assertThat(opiniones().buscarAprobadas(KAYAK_BAHIA_TAGANGA, Paginacion.de(null, null)).contenido())
                .as("la del kayak sigue en moderación").isEmpty();
    }

    @Test
    @DisplayName("resume las aprobadas: cuántas son y la suma de sus calificaciones")
    default void resumen() {
        ResumenCalificaciones caminata = opiniones().resumirAprobadas(CAMINATA_CABO_SAN_JUAN);

        assertThat(caminata).isEqualTo(new ResumenCalificaciones(3, 14));
        assertThat(caminata.promedio()).contains(new BigDecimal("4.7"));
        assertThat(opiniones().resumirAprobadas(PAQUETE_TAYRONA_MAR_Y_SELVA)).isEqualTo(new ResumenCalificaciones(2, 9));
        assertThat(opiniones().resumirAprobadas(KAYAK_BAHIA_TAGANGA)).isEqualTo(ResumenCalificaciones.SIN_OPINIONES);
    }

    @Test
    @DisplayName("lee las opiniones del turista en cualquier estado, de la más reciente a la más antigua")
    default void delTurista() {
        assertThat(opiniones().buscarDelTurista(ANDRES, Paginacion.de(null, null)).contenido())
                .extracting(Opinion::id)
                .containsExactly(OPINION_ANDRES_KAYAK, "d00000000000000000000004");
        assertThat(opiniones().buscarDelTurista(UUID.randomUUID(), Paginacion.de(null, null)).totalElementos())
                .isZero();
    }

    @Test
    @DisplayName("la cola de moderación va de la más antigua a la más reciente, sin aprobadas ni rechazadas")
    default void enModeracion() {
        Pagina<Opinion> pendientes = opiniones().buscarEnModeracion(Paginacion.de(null, null));

        assertThat(pendientes.contenido()).extracting(Opinion::id)
                .containsExactly(OPINION_EN_MODERACION_CAFE, OPINION_ANDRES_KAYAK);
        assertThat(pendientes.totalElementos()).isEqualTo(2);
    }

    @Test
    @DisplayName("borra todas las opiniones de un turista y solo las suyas (DC-44)")
    default void eliminarDelTurista() {
        assertThat(opiniones().eliminarDelTurista(ANDRES)).isEqualTo(2);

        assertThat(opiniones().buscarDelTurista(ANDRES, Paginacion.de(null, null)).totalElementos()).isZero();
        assertThat(opiniones().buscarPorId(OPINION_ANDRES_KAYAK)).isEmpty();
        assertThat(opiniones().buscarPorId(OPINION_LAURA_CABO_SAN_JUAN)).isPresent();
        assertThat(opiniones().eliminarDelTurista(ANDRES)).isZero();
    }

    @Test
    @DisplayName("una habilitación se registra una sola vez; repetirla no falla (DC-40)")
    default void registrarHabilitacion() {
        HabilitacionOpinion nueva = new HabilitacionOpinion(ANDRES, RECORRIDO_CENTRO_HISTORICO);

        assertThat(habilitaciones().existe(ANDRES, RECORRIDO_CENTRO_HISTORICO)).isFalse();
        assertThat(habilitaciones().registrar(nueva)).isTrue();
        assertThat(habilitaciones().registrar(nueva)).isFalse();
        assertThat(habilitaciones().existe(ANDRES, RECORRIDO_CENTRO_HISTORICO)).isTrue();
        assertThat(habilitaciones().existe(ANDRES, "no-es-un-objectid")).isFalse();
    }

    @Test
    @DisplayName("las habilitaciones de demostración incluyen las ofertas que Laura pagó y aún no calificó")
    default void habilitacionesDeDemostracion() {
        assertThat(habilitaciones().existe(LAURA, CAMINATA_CABO_SAN_JUAN)).isTrue();
        assertThat(habilitaciones().existe(LAURA, SNORKEL_PLAYA_CRISTAL)).isTrue();
        assertThat(habilitaciones().existe(LAURA, AVISTAMIENTO_AVES_MINCA)).isTrue();
        assertThat(habilitaciones().existe(LAURA, KAYAK_BAHIA_TAGANGA)).isFalse();
    }

    @Test
    @DisplayName("borra todas las habilitaciones de un turista y solo las suyas (DC-44)")
    default void eliminarHabilitaciones() {
        assertThat(habilitaciones().eliminarDelTurista(LAURA)).isEqualTo(3);

        assertThat(habilitaciones().existe(LAURA, SNORKEL_PLAYA_CRISTAL)).isFalse();
        assertThat(habilitaciones().existe(ANDRES, KAYAK_BAHIA_TAGANGA)).isTrue();
    }
}
