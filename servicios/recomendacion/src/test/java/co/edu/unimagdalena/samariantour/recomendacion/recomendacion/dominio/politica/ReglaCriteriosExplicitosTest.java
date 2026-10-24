package co.edu.unimagdalena.samariantour.recomendacion.recomendacion.dominio.politica;

import co.edu.unimagdalena.samariantour.recomendacion.recomendacion.dominio.modelo.CriterioAfinidad;
import co.edu.unimagdalena.samariantour.recomendacion.recomendacion.dominio.modelo.OfertaCandidata;
import co.edu.unimagdalena.samariantour.recomendacion.recomendacion.dominio.modelo.OfertaEvaluada;
import co.edu.unimagdalena.samariantour.recomendacion.recomendacion.dominio.modelo.Preferencias;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static co.edu.unimagdalena.samariantour.recomendacion.recomendacion.OfertasDePrueba.MINCA;
import static co.edu.unimagdalena.samariantour.recomendacion.recomendacion.OfertasDePrueba.TAGANGA;
import static co.edu.unimagdalena.samariantour.recomendacion.recomendacion.OfertasDePrueba.oferta;
import static co.edu.unimagdalena.samariantour.recomendacion.recomendacion.OfertasDePrueba.preferencias;
import static co.edu.unimagdalena.samariantour.recomendacion.recomendacion.dominio.modelo.CriterioAfinidad.ACCESIBILIDAD;
import static co.edu.unimagdalena.samariantour.recomendacion.recomendacion.dominio.modelo.CriterioAfinidad.NIVEL_ESFUERZO;
import static co.edu.unimagdalena.samariantour.recomendacion.recomendacion.dominio.modelo.CriterioAfinidad.PRECIO;
import static co.edu.unimagdalena.samariantour.recomendacion.recomendacion.dominio.modelo.CriterioAfinidad.TIPO_ACTIVIDAD;
import static co.edu.unimagdalena.samariantour.recomendacion.recomendacion.dominio.modelo.CriterioAfinidad.TIPO_OFERTA;
import static co.edu.unimagdalena.samariantour.recomendacion.recomendacion.dominio.modelo.CriterioAfinidad.ZONA;
import static co.edu.unimagdalena.samariantour.recomendacion.recomendacion.dominio.modelo.NivelEsfuerzo.ALTA_EXIGENCIA;
import static co.edu.unimagdalena.samariantour.recomendacion.recomendacion.dominio.modelo.NivelEsfuerzo.BAJO;
import static co.edu.unimagdalena.samariantour.recomendacion.recomendacion.dominio.modelo.NivelEsfuerzo.MODERADO;
import static co.edu.unimagdalena.samariantour.recomendacion.recomendacion.dominio.modelo.TipoActividad.AVISTAMIENTO_FAUNA;
import static co.edu.unimagdalena.samariantour.recomendacion.recomendacion.dominio.modelo.TipoActividad.BUCEO_SNORKEL;
import static co.edu.unimagdalena.samariantour.recomendacion.recomendacion.dominio.modelo.TipoActividad.SENDERISMO;
import static co.edu.unimagdalena.samariantour.recomendacion.recomendacion.dominio.modelo.TipoOferta.ACTIVIDAD;
import static co.edu.unimagdalena.samariantour.recomendacion.recomendacion.dominio.modelo.TipoOferta.RESTAURANTE;
import static org.assertj.core.api.Assertions.assertThat;

class ReglaCriteriosExplicitosTest {

    private final ReglaCriteriosExplicitos regla = new ReglaCriteriosExplicitos();

    @Nested
    @DisplayName("filtros duros")
    class FiltrosDuros {

        @Test
        @DisplayName("quien pide accesibilidad solo recibe ofertas accesibles")
        void accesibilidad() {
            Preferencias accesible = preferencias().accesible().crear();
            assertThat(regla.pasaFiltrosDuros(oferta("1").accesible(true).crear(), accesible)).isTrue();
            assertThat(regla.pasaFiltrosDuros(oferta("2").accesible(false).crear(), accesible)).isFalse();
            assertThat(regla.pasaFiltrosDuros(oferta("3").accesible(false).crear(),
                    preferencias().texto("algo").crear())).isTrue();
        }

        @Test
        @DisplayName("el precio igual al máximo pasa; uno mayor no")
        void precio() {
            Preferencias hasta80 = preferencias().precioMaximo(80_000).crear();
            assertThat(regla.pasaFiltrosDuros(oferta("1").precio(80_000).crear(), hasta80)).isTrue();
            assertThat(regla.pasaFiltrosDuros(oferta("2").precio(80_001).crear(), hasta80)).isFalse();
        }

        @Test
        @DisplayName("esfuerzo en el orden BAJO < MODERADO < ALTA_EXIGENCIA; una oferta sin nivel pasa")
        void esfuerzo() {
            Preferencias hastaModerado = preferencias().nivelMaximo(MODERADO).crear();
            assertThat(regla.pasaFiltrosDuros(oferta("1").nivel(BAJO).crear(), hastaModerado)).isTrue();
            assertThat(regla.pasaFiltrosDuros(oferta("2").nivel(MODERADO).crear(), hastaModerado)).isTrue();
            assertThat(regla.pasaFiltrosDuros(oferta("3").nivel(ALTA_EXIGENCIA).crear(), hastaModerado)).isFalse();
            assertThat(regla.pasaFiltrosDuros(oferta("4").tipo(RESTAURANTE).nivel(null).crear(), hastaModerado))
                    .isTrue();
        }

        @Test
        @DisplayName("evaluar deja fuera las que no pasan y conserva el orden de las demás")
        void evaluarFiltra() {
            List<OfertaEvaluada> evaluadas = regla.evaluar(List.of(
                    oferta("1").precio(50_000).crear(),
                    oferta("2").precio(150_000).crear(),
                    oferta("3").precio(70_000).crear()), preferencias().precioMaximo(100_000).crear());
            assertThat(evaluadas).extracting(e -> e.oferta().id()).containsExactly("1", "3");
        }
    }

    @Nested
    @DisplayName("afinidad y criterios cumplidos")
    class Afinidad {

        @Test
        @DisplayName("un punto por tipo de oferta, por algún tipo de actividad y por zona")
        void unPuntoPorCriterioBlando() {
            Preferencias preferencias = preferencias().tipos(ACTIVIDAD).actividades(BUCEO_SNORKEL, SENDERISMO)
                    .zona(MINCA).crear();
            assertThat(afinidad(oferta("1").crear(), preferencias)).isEqualTo(3);
            assertThat(afinidad(oferta("2").zona(TAGANGA, "Taganga").crear(), preferencias)).isEqualTo(2);
            assertThat(afinidad(oferta("3").actividades(AVISTAMIENTO_FAUNA).zona(TAGANGA, "Taganga").crear(),
                    preferencias)).isEqualTo(1);
            assertThat(afinidad(oferta("4").tipo(RESTAURANTE).actividades().zona(TAGANGA, "Taganga").crear(),
                    preferencias)).isZero();
        }

        @Test
        @DisplayName("los filtros duros que pidió el turista cuentan como criterios cumplidos, pero no suman afinidad")
        void durosParaLaExplicacion() {
            Preferencias preferencias = preferencias().accesible().nivelMaximo(MODERADO).precioMaximo(90_000)
                    .crear();
            OfertaEvaluada evaluada = regla.evaluar(List.of(oferta("1").nivel(BAJO).crear()), preferencias).getFirst();
            assertThat(evaluada.criteriosCumplidos()).containsExactly(ACCESIBILIDAD, NIVEL_ESFUERZO, PRECIO);
            assertThat(evaluada.afinidad()).isZero();
        }

        @Test
        @DisplayName("una oferta sin nivel pasa el filtro de esfuerzo, pero no lo cumple como criterio")
        void sinNivelNoCumpleEsfuerzo() {
            Preferencias preferencias = preferencias().nivelMaximo(BAJO).crear();
            assertThat(regla.criteriosCumplidos(oferta("1").tipo(RESTAURANTE).nivel(null).crear(), preferencias))
                    .isEmpty();
        }

        @Test
        @DisplayName("lo que el turista no pidió no se cuenta: una oferta accesible no cumple ACCESIBILIDAD si no la pidió")
        void soloLoPedido() {
            assertThat(regla.criteriosCumplidos(oferta("1").crear(), preferencias().texto("aves").crear())).isEmpty();
        }

        private int afinidad(OfertaCandidata oferta, Preferencias preferencias) {
            return regla.evaluar(List.of(oferta), preferencias).getFirst().afinidad();
        }
    }

    @Nested
    @DisplayName("criterios que cita la IA")
    class CriteriosDeLaIA {

        @Test
        @DisplayName("con filtros elegidos, el criterio blando tiene que coincidir con ellos")
        void blandoConFiltro() {
            Preferencias buceo = preferencias().actividades(BUCEO_SNORKEL).zona(TAGANGA).crear();
            OfertaCandidata caminataEnMinca = oferta("1").crear();
            assertThat(regla.esCierto(TIPO_ACTIVIDAD, caminataEnMinca, buceo)).isFalse();
            assertThat(regla.esCierto(ZONA, caminataEnMinca, buceo)).isFalse();
            assertThat(regla.esCierto(TIPO_OFERTA, caminataEnMinca, buceo)).isFalse();
        }

        @Test
        @DisplayName("sin el filtro, un criterio blando se acepta si el turista escribió texto (la IA lo dedujo)")
        void blandoDeducidoDelTexto() {
            OfertaCandidata caminata = oferta("1").crear();
            Preferencias soloTexto = preferencias().texto("quiero caminar por la sierra").crear();
            assertThat(regla.esCierto(TIPO_ACTIVIDAD, caminata, soloTexto)).isTrue();
            assertThat(regla.esCierto(ZONA, caminata, soloTexto)).isTrue();
            assertThat(regla.esCierto(TIPO_ACTIVIDAD, oferta("2").tipo(RESTAURANTE).actividades().crear(), soloTexto))
                    .as("un restaurante no tiene tipo de actividad").isFalse();
            assertThat(regla.esCierto(ZONA, caminata, preferencias().precioMaximo(90_000).crear()))
                    .as("sin texto ni zona elegida no hay de dónde deducir la zona").isFalse();
        }

        @Test
        @DisplayName("los duros se comprueban contra la oferta: accesible, dentro del precio y del esfuerzo pedidos")
        void durosContraLaOferta() {
            Preferencias preferencias = preferencias().texto("barato").nivelMaximo(BAJO).crear();
            assertThat(regla.esCierto(ACCESIBILIDAD, oferta("1").accesible(true).crear(), preferencias)).isTrue();
            assertThat(regla.esCierto(ACCESIBILIDAD, oferta("2").accesible(false).crear(), preferencias)).isFalse();
            assertThat(regla.esCierto(PRECIO, oferta("3").crear(), preferencias))
                    .as("el turista no dio un precio máximo").isFalse();
            assertThat(regla.esCierto(NIVEL_ESFUERZO, oferta("4").nivel(BAJO).crear(), preferencias)).isTrue();
            assertThat(regla.esCierto(NIVEL_ESFUERZO, oferta("5").nivel(MODERADO).crear(), preferencias)).isFalse();
        }

        @Test
        @DisplayName("todo criterio cumplido por las reglas también es cierto para la validación de la IA")
        void coherenteConLasReglas() {
            Preferencias preferencias = preferencias().tipos(ACTIVIDAD).actividades(SENDERISMO).zona(MINCA)
                    .accesible().nivelMaximo(MODERADO).precioMaximo(100_000).crear();
            OfertaCandidata caminata = oferta("1").crear();
            for (CriterioAfinidad criterio : regla.criteriosCumplidos(caminata, preferencias)) {
                assertThat(regla.esCierto(criterio, caminata, preferencias)).as(criterio.name()).isTrue();
            }
            assertThat(regla.criteriosCumplidos(caminata, preferencias)).hasSize(CriterioAfinidad.values().length);
        }
    }
}
