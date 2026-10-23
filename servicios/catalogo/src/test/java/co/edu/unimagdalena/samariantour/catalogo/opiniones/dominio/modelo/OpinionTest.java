package co.edu.unimagdalena.samariantour.catalogo.opiniones.dominio.modelo;

import co.edu.unimagdalena.samariantour.catalogo.opiniones.dominio.excepcion.DatoInvalidoException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OpinionTest {

    private static final UUID TURISTA = UUID.fromString("70000000-0000-0000-0000-000000000007");
    private static final Instant ENVIADA = Instant.parse("2026-10-20T15:00:00Z");
    private static final Instant REVISADA = Instant.parse("2026-10-21T09:30:00Z");
    private static final String COMENTARIO = "El guía conocía muy bien el sendero y cuidó el grupo.";

    private static Opinion enviada() {
        return Opinion.enviar("d00000000000000000000001", "b00000000000000000000001", TURISTA, 5, COMENTARIO, ENVIADA);
    }

    @Nested
    @DisplayName("Enviar")
    class Enviar {

        @Test
        @DisplayName("una opinión nueva nace en moderación, sin revisión ni motivo")
        void naceEnModeracion() {
            Opinion opinion = enviada();

            assertThat(opinion.estado()).isEqualTo(EstadoOpinion.EN_MODERACION);
            assertThat(opinion.estaEnModeracion()).isTrue();
            assertThat(opinion.estaAprobada()).isFalse();
            assertThat(opinion.revisadaEn()).isNull();
            assertThat(opinion.motivoRechazo()).isNull();
            assertThat(opinion.revision()).isEqualTo(Opinion.REVISION_NUEVA);
            assertThat(opinion.fecha()).isEqualTo(ENVIADA);
        }

        @ParameterizedTest
        @ValueSource(ints = {0, 6, -1})
        @DisplayName("la calificación va de 1 a 5")
        void calificacionFueraDeRango(int calificacion) {
            assertThatThrownBy(() -> Opinion.enviar("d1", "b1", TURISTA, calificacion, COMENTARIO, ENVIADA))
                    .isInstanceOf(DatoInvalidoException.class)
                    .hasMessage("calificacion: debe estar entre 1 y 5");
        }

        @Test
        @DisplayName("la calificación es obligatoria")
        void sinCalificacion() {
            assertThatThrownBy(() -> Opinion.enviar("d1", "b1", TURISTA, null, COMENTARIO, ENVIADA))
                    .isInstanceOf(DatoInvalidoException.class)
                    .hasMessage("calificacion: es obligatorio");
        }

        @Test
        @DisplayName("el comentario tiene de 10 a 1000 caracteres, sin contar los espacios de los extremos")
        void largoDelComentario() {
            assertThatThrownBy(() -> Opinion.enviar("d1", "b1", TURISTA, 4, "   Muy bien   ", ENVIADA))
                    .isInstanceOf(DatoInvalidoException.class)
                    .hasMessage("comentario: debe tener entre 10 y 1000 caracteres");
            assertThatThrownBy(() -> Opinion.enviar("d1", "b1", TURISTA, 4, "x".repeat(1001), ENVIADA))
                    .isInstanceOf(DatoInvalidoException.class);
            assertThatThrownBy(() -> Opinion.enviar("d1", "b1", TURISTA, 4, "   ", ENVIADA))
                    .isInstanceOf(DatoInvalidoException.class)
                    .hasMessage("comentario: es obligatorio");

            assertThat(Opinion.enviar("d1", "b1", TURISTA, 4, "  " + "x".repeat(10) + "  ", ENVIADA).comentario())
                    .isEqualTo("x".repeat(10));
            assertThat(Opinion.enviar("d1", "b1", TURISTA, 4, "x".repeat(1000), ENVIADA).comentario()).hasSize(1000);
        }
    }

    @Nested
    @DisplayName("Moderar")
    class Moderar {

        @Test
        @DisplayName("aprobar la publica y guarda cuándo se revisó")
        void aprobar() {
            Opinion opinion = enviada();

            assertThat(opinion.aprobar(REVISADA)).isTrue();

            assertThat(opinion.estaAprobada()).isTrue();
            assertThat(opinion.revisadaEn()).isEqualTo(REVISADA);
        }

        @Test
        @DisplayName("rechazar guarda el motivo, que el turista ve")
        void rechazar() {
            Opinion opinion = enviada();

            assertThat(opinion.rechazar("  Incluye datos personales de terceros  ", REVISADA)).isTrue();

            assertThat(opinion.estado()).isEqualTo(EstadoOpinion.RECHAZADA);
            assertThat(opinion.motivoRechazo()).isEqualTo("Incluye datos personales de terceros");
            assertThat(opinion.revisadaEn()).isEqualTo(REVISADA);
        }

        @Test
        @DisplayName("rechazar exige un motivo")
        void rechazarSinMotivo() {
            Opinion opinion = enviada();

            assertThatThrownBy(() -> opinion.rechazar(" ", REVISADA))
                    .isInstanceOf(DatoInvalidoException.class)
                    .hasMessage("motivo: es obligatorio al rechazar");
            assertThat(opinion.estaEnModeracion()).isTrue();
        }

        @Test
        @DisplayName("una opinión que ya se moderó no cambia: la decisión repetida o atrasada se ignora (DC-08)")
        void moderacionIdempotente() {
            Opinion aprobada = enviada();
            aprobada.aprobar(REVISADA);

            assertThat(aprobada.aprobar(REVISADA.plusSeconds(60))).isFalse();
            assertThat(aprobada.rechazar("Tarde", REVISADA.plusSeconds(60))).isFalse();
            assertThat(aprobada.estaAprobada()).isTrue();
            assertThat(aprobada.revisadaEn()).isEqualTo(REVISADA);

            Opinion rechazada = enviada();
            rechazada.rechazar("Lenguaje ofensivo", REVISADA);

            assertThat(rechazada.aprobar(REVISADA.plusSeconds(60))).isFalse();
            assertThat(rechazada.estado()).isEqualTo(EstadoOpinion.RECHAZADA);
            assertThat(rechazada.motivoRechazo()).isEqualTo("Lenguaje ofensivo");
        }
    }

    @Nested
    @DisplayName("Reconstituir")
    class Reconstituir {

        @Test
        @DisplayName("devuelve la opinión guardada con su estado y su revisión")
        void reconstituir() {
            Opinion opinion = Opinion.reconstituir("d1", "b1", TURISTA, 3, COMENTARIO, ENVIADA,
                    EstadoOpinion.RECHAZADA, "Contiene publicidad", REVISADA, 4);

            assertThat(opinion.estado()).isEqualTo(EstadoOpinion.RECHAZADA);
            assertThat(opinion.motivoRechazo()).isEqualTo("Contiene publicidad");
            assertThat(opinion.revision()).isEqualTo(4);
        }

        @Test
        @DisplayName("un estado incoherente con la revisión o el motivo no se reconstituye")
        void estadoIncoherente() {
            assertThatThrownBy(() -> Opinion.reconstituir("d1", "b1", TURISTA, 3, COMENTARIO, ENVIADA,
                    EstadoOpinion.APROBADA, null, null, 1)).isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> Opinion.reconstituir("d1", "b1", TURISTA, 3, COMENTARIO, ENVIADA,
                    EstadoOpinion.RECHAZADA, null, REVISADA, 1)).isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> Opinion.reconstituir("d1", "b1", TURISTA, 3, COMENTARIO, ENVIADA,
                    EstadoOpinion.EN_MODERACION, null, REVISADA, 1)).isInstanceOf(IllegalArgumentException.class);
        }
    }
}
