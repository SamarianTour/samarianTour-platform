package co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.politica;

import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.excepcion.DatoInvalidoException;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.NivelEsfuerzo;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.Oferta;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.PrestadorCatalogo;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.TipoActividad;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.TipoPrestador;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.DatosDePrueba.AHORA;
import static co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.DatosDePrueba.OTRO_PRESTADOR;
import static co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.DatosDePrueba.PRESTADOR;
import static co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.DatosDePrueba.actividad;
import static co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.DatosDePrueba.actividadCompletaDeGuia;
import static co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.DatosDePrueba.actividadPublicada;
import static co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.DatosDePrueba.alojamiento;
import static co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.DatosDePrueba.habilitado;
import static co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.DatosDePrueba.paquete;
import static co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.DatosDePrueba.zonaActiva;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ReglasPaqueteTest {

    private final Oferta caminata = actividadPublicada("caminata", NivelEsfuerzo.BAJO, true);
    private final Oferta buceo = actividadPublicada("buceo", NivelEsfuerzo.ALTA_EXIGENCIA, true);

    @Nested
    @DisplayName("Actividades del paquete (RF-22)")
    class Actividades {

        @Test
        @DisplayName("acepta actividades del mismo prestador")
        void delMismoPrestador() {
            assertThatCode(() -> ReglasPaquete.verificarActividades(PRESTADOR, paquete("caminata", "buceo"),
                    List.of(caminata, buceo)))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("rechaza una actividad que no existe")
        void inexistente() {
            assertThatThrownBy(() -> ReglasPaquete.verificarActividades(PRESTADOR, paquete("caminata", "buceo"),
                    List.of(caminata)))
                    .isInstanceOf(DatoInvalidoException.class)
                    .hasMessageContaining("buceo");
        }

        @Test
        @DisplayName("rechaza una actividad de otro prestador")
        void deOtroPrestador() {
            Oferta ajena = actividadPublicada("ajena", new PrestadorCatalogo(OTRO_PRESTADOR, true, 1),
                    NivelEsfuerzo.BAJO, true);

            assertThatThrownBy(() -> ReglasPaquete.verificarActividades(PRESTADOR, paquete("caminata", "ajena"),
                    List.of(caminata, ajena)))
                    .isInstanceOf(DatoInvalidoException.class)
                    .hasMessageContaining("ajena");
        }

        @Test
        @DisplayName("rechaza una oferta que no es una actividad")
        void noEsActividad() {
            Oferta cabana = Oferta.publicar("cabana", habilitado(), TipoPrestador.ALOJAMIENTO, alojamiento(),
                    zonaActiva(), AHORA);

            assertThatThrownBy(() -> ReglasPaquete.verificarActividades(PRESTADOR, paquete("caminata", "cabana"),
                    List.of(caminata, cabana)))
                    .isInstanceOf(DatoInvalidoException.class)
                    .hasMessageContaining("cabana");
        }

        @Test
        @DisplayName("rechaza una actividad retirada: el retiro es definitivo (DC-23)")
        void retirada() {
            buceo.retirar(PRESTADOR);

            assertThatThrownBy(() -> ReglasPaquete.verificarActividades(PRESTADOR, paquete("caminata", "buceo"),
                    List.of(caminata, buceo)))
                    .isInstanceOfSatisfying(DatoInvalidoException.class, excepcion -> {
                        assertThat(excepcion.codigo()).isEqualTo("DATO_INVALIDO");
                        assertThat(excepcion).hasMessageContaining("buceo").hasMessageContaining("retirada");
                    });
        }

        @Test
        @DisplayName("acepta una actividad aún sin aprobar, en moderación o rechazada (DC-23)")
        void sinAprobar() {
            Oferta enModeracion = Oferta.publicar("en-moderacion", habilitado(), TipoPrestador.AGENCIA_OPERADORA,
                    actividad(), zonaActiva(), AHORA);
            Oferta rechazada = Oferta.publicar("rechazada", habilitado(), TipoPrestador.AGENCIA_OPERADORA,
                    actividad(), zonaActiva(), AHORA);
            rechazada.rechazar(1, "Falta el punto de encuentro", AHORA);

            assertThatCode(() -> ReglasPaquete.verificarActividades(PRESTADOR,
                    paquete("en-moderacion", "rechazada"), List.of(enModeracion, rechazada)))
                    .doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("Esfuerzo y accesibilidad (RF-17)")
    class EsfuerzoYAccesibilidad {

        @Test
        @DisplayName("el paquete toma el mayor nivel de esfuerzo de sus actividades")
        void mayorNivelDeEsfuerzo() {
            Oferta kayak = actividadPublicada("kayak", NivelEsfuerzo.MODERADO, true);

            assertThat(ReglasPaquete.nivelEsfuerzo(List.of(caminata, kayak))).isEqualTo(NivelEsfuerzo.MODERADO);
            assertThat(ReglasPaquete.nivelEsfuerzo(List.of(caminata, buceo, kayak)))
                    .isEqualTo(NivelEsfuerzo.ALTA_EXIGENCIA);
        }

        @Test
        @DisplayName("el paquete es accesible en silla de ruedas solo si todas sus actividades lo son")
        void accesibleSiTodasLoSon() {
            Oferta escalada = actividadPublicada("escalada", NivelEsfuerzo.ALTA_EXIGENCIA, false);

            assertThat(ReglasPaquete.esAccesibleSillaRuedas(List.of(caminata, buceo))).isTrue();
            assertThat(ReglasPaquete.esAccesibleSillaRuedas(List.of(caminata, escalada))).isFalse();
        }

        @Test
        @DisplayName("los tipos de actividad del paquete son los de sus actividades, sin repetir (DC-24)")
        void tiposDeActividad() {
            Oferta aves = Oferta.publicar("aves", habilitado(), TipoPrestador.GUIA_INDEPENDIENTE,
                    actividadCompletaDeGuia(), zonaActiva(), AHORA);
            aves.aprobar(1, habilitado(), AHORA);

            assertThat(ReglasPaquete.tiposActividad(List.of(caminata, buceo, aves)))
                    .containsExactlyInAnyOrder(TipoActividad.SENDERISMO, TipoActividad.AVISTAMIENTO_FAUNA);
        }

        @Test
        @DisplayName("el cálculo usa la versión publicada de cada actividad")
        void exigeActividadesPublicadas() {
            Oferta sinAprobar = Oferta.publicar("sin-aprobar", habilitado(), TipoPrestador.AGENCIA_OPERADORA,
                    actividad(), zonaActiva(), AHORA);

            assertThatThrownBy(() -> ReglasPaquete.nivelEsfuerzo(List.of(caminata, sinAprobar)))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("Visibilidad (RF-22)")
    class Visibilidad {

        @Test
        @DisplayName("un paquete publicado es visible si todas sus actividades son visibles")
        void visible() {
            assertThat(ReglasPaquete.esVisible(paquetePublicado(), List.of(caminata, buceo))).isTrue();
        }

        @Test
        @DisplayName("deja de ser visible cuando se retira una de sus actividades")
        void actividadRetirada() {
            Oferta paquete = paquetePublicado();

            buceo.retirar(PRESTADOR);

            assertThat(paquete.estaVisible()).isTrue();
            assertThat(ReglasPaquete.esVisible(paquete, List.of(caminata, buceo))).isFalse();
        }

        @Test
        @DisplayName("no es visible si falta cargar una de sus actividades")
        void actividadFaltante() {
            assertThat(ReglasPaquete.esVisible(paquetePublicado(), List.of(caminata))).isFalse();
        }

        @Test
        @DisplayName("un paquete en moderación no es visible aunque sus actividades lo sean")
        void paqueteEnModeracion() {
            Oferta enModeracion = Oferta.publicar("paquete-1", habilitado(), TipoPrestador.AGENCIA_OPERADORA,
                    paquete("caminata", "buceo"), zonaActiva(), List.of(caminata, buceo), AHORA);

            assertThat(ReglasPaquete.esVisible(enModeracion, List.of(caminata, buceo))).isFalse();
        }

        @Test
        @DisplayName("para una oferta que no es un paquete equivale a su propia visibilidad")
        void ofertaQueNoEsPaquete() {
            assertThat(ReglasPaquete.actividadIdsDe(caminata)).isEmpty();
            assertThat(ReglasPaquete.esVisible(caminata, List.of())).isTrue();
        }

        private Oferta paquetePublicado() {
            Oferta paquete = Oferta.publicar("paquete-1", habilitado(), TipoPrestador.AGENCIA_OPERADORA,
                    paquete("caminata", "buceo"), zonaActiva(), List.of(caminata, buceo), AHORA);
            paquete.aprobar(1, habilitado(), AHORA);
            return paquete;
        }
    }
}
