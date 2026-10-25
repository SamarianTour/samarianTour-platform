package co.edu.unimagdalena.samariantour.recomendacion.recomendacion.dominio.politica;

import co.edu.unimagdalena.samariantour.recomendacion.recomendacion.dominio.modelo.Idioma;
import co.edu.unimagdalena.samariantour.recomendacion.recomendacion.dominio.modelo.OfertaCandidata;
import co.edu.unimagdalena.samariantour.recomendacion.recomendacion.dominio.modelo.Recomendacion;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.stream.IntStream;

import static co.edu.unimagdalena.samariantour.recomendacion.recomendacion.OfertasDePrueba.TAGANGA;
import static co.edu.unimagdalena.samariantour.recomendacion.recomendacion.OfertasDePrueba.oferta;
import static co.edu.unimagdalena.samariantour.recomendacion.recomendacion.dominio.modelo.CriterioAfinidad.ACCESIBILIDAD;
import static co.edu.unimagdalena.samariantour.recomendacion.recomendacion.dominio.modelo.CriterioAfinidad.NIVEL_ESFUERZO;
import static co.edu.unimagdalena.samariantour.recomendacion.recomendacion.dominio.modelo.CriterioAfinidad.PRECIO;
import static co.edu.unimagdalena.samariantour.recomendacion.recomendacion.dominio.modelo.CriterioAfinidad.TIPO_ACTIVIDAD;
import static co.edu.unimagdalena.samariantour.recomendacion.recomendacion.dominio.modelo.CriterioAfinidad.TIPO_OFERTA;
import static co.edu.unimagdalena.samariantour.recomendacion.recomendacion.dominio.modelo.NivelEsfuerzo.ALTA_EXIGENCIA;
import static co.edu.unimagdalena.samariantour.recomendacion.recomendacion.dominio.modelo.NivelEsfuerzo.BAJO;
import static co.edu.unimagdalena.samariantour.recomendacion.recomendacion.dominio.modelo.TipoActividad.AVISTAMIENTO_FAUNA;
import static co.edu.unimagdalena.samariantour.recomendacion.recomendacion.dominio.modelo.TipoActividad.BUCEO_SNORKEL;
import static co.edu.unimagdalena.samariantour.recomendacion.recomendacion.dominio.modelo.TipoActividad.SENDERISMO;
import static co.edu.unimagdalena.samariantour.recomendacion.recomendacion.dominio.modelo.TipoOferta.RESTAURANTE;
import static org.assertj.core.api.Assertions.assertThat;

class ReglaAlternativasTest {

    private static final String TAYRONA = "a00000000000000000000005";

    /** Caminata moderada en Tayrona (alta afluencia), accesible, de 80.000 pesos, de senderismo y fauna. */
    private final OfertaCandidata origen = oferta("origen").zona(TAYRONA, "Parque Tayrona").altaAfluencia()
            .actividades(SENDERISMO, AVISTAMIENTO_FAUNA).crear();

    private final ReglaAlternativas regla = new ReglaAlternativas(new PoliticaRotacion(new Random(7)));

    @Nested
    @DisplayName("candidatas")
    class Candidatas {

        @Test
        @DisplayName("del mismo tipo, en zonas sin alta afluencia y distintas de la de origen")
        void filtros() {
            List<OfertaCandidata> visibles = List.of(origen,
                    oferta("otra-de-tayrona").zona(TAYRONA, "Parque Tayrona").altaAfluencia().crear(),
                    oferta("restaurante").tipo(RESTAURANTE).nivel(null).crear(),
                    oferta("en-minca").crear());

            assertThat(ids(regla.proponer(origen, visibles, Idioma.ES))).containsExactly("en-minca");
        }

        @Test
        @DisplayName("si la de origen es accesible, las no accesibles quedan fuera aunque sean muy afines")
        void accesibilidad() {
            List<OfertaCandidata> visibles = List.of(origen, oferta("no-accesible").accesible(false).crear(),
                    oferta("accesible").actividades(BUCEO_SNORKEL).nivel(ALTA_EXIGENCIA).precio(500_000).crear());

            List<Recomendacion> alternativas = regla.proponer(origen, visibles, Idioma.ES);

            assertThat(ids(alternativas)).containsExactly("accesible");
            assertThat(alternativas.getFirst().explicacion().criterios()).containsExactly(TIPO_OFERTA, ACCESIBILIDAD);
        }

        @Test
        @DisplayName("si la de origen no es accesible, entran las dos y ninguna lleva el criterio de accesibilidad")
        void origenNoAccesible() {
            OfertaCandidata noAccesible = oferta("origen").zona(TAYRONA, "Parque Tayrona").altaAfluencia()
                    .accesible(false).crear();
            List<OfertaCandidata> visibles = List.of(noAccesible, oferta("a").accesible(false).crear(),
                    oferta("b").crear());

            assertThat(regla.proponer(noAccesible, visibles, Idioma.ES))
                    .hasSize(2)
                    .allSatisfy(r -> assertThat(r.explicacion().criterios()).doesNotContain(ACCESIBILIDAD));
        }

        @Test
        @DisplayName("sin candidatas, la lista va vacía; con una sola, solo esa (el aviso lo pone el caso de uso)")
        void pocas() {
            assertThat(regla.proponer(origen, List.of(origen), Idioma.ES)).isEmpty();
            assertThat(regla.proponer(origen, List.of(origen, oferta("unica").crear()), Idioma.ES)).hasSize(1);
        }
    }

    @Nested
    @DisplayName("afinidad con la de origen")
    class Afinidad {

        @Test
        @DisplayName("un punto por actividad compartida, esfuerzo igual o menor y precio dentro de ±50 %")
        void criterios() {
            assertThat(ReglaAlternativas.evaluar(origen, oferta("todo").nivel(BAJO).crear()).afinidad()).isEqualTo(3);
            assertThat(ReglaAlternativas.evaluar(origen, oferta("todo").crear()).criteriosCumplidos())
                    .containsExactly(TIPO_OFERTA, TIPO_ACTIVIDAD, ACCESIBILIDAD, NIVEL_ESFUERZO, PRECIO);
            assertThat(ReglaAlternativas.evaluar(origen,
                    oferta("nada").actividades(BUCEO_SNORKEL).nivel(ALTA_EXIGENCIA).precio(200_000).crear()))
                    .satisfies(evaluada -> {
                        assertThat(evaluada.afinidad()).isZero();
                        assertThat(evaluada.criteriosCumplidos()).containsExactly(TIPO_OFERTA, ACCESIBILIDAD);
                    });
        }

        @Test
        @DisplayName("el precio entra justo en el 50 % por debajo y por encima, y no un peso más allá")
        void bordesDelPrecio() {
            BigDecimal ochenta = new BigDecimal("80000");

            assertThat(ReglaAlternativas.precioParecido(ochenta, new BigDecimal("40000"))).isTrue();
            assertThat(ReglaAlternativas.precioParecido(ochenta, new BigDecimal("120000"))).isTrue();
            assertThat(ReglaAlternativas.precioParecido(ochenta, new BigDecimal("39999"))).isFalse();
            assertThat(ReglaAlternativas.precioParecido(ochenta, new BigDecimal("120001"))).isFalse();
            assertThat(ReglaAlternativas.precioParecido(BigDecimal.ZERO, BigDecimal.ZERO)).isTrue();
            assertThat(ReglaAlternativas.precioParecido(BigDecimal.ZERO, BigDecimal.ONE)).isFalse();
        }

        @Test
        @DisplayName("sin nivel de esfuerzo en alguna de las dos (restaurantes, alojamientos), no suma por esfuerzo")
        void sinEsfuerzo() {
            OfertaCandidata restaurante = oferta("origen").tipo(RESTAURANTE).zona(TAYRONA, "Parque Tayrona")
                    .altaAfluencia().nivel(null).crear();

            assertThat(ReglaAlternativas.evaluar(restaurante, oferta("otro").tipo(RESTAURANTE).nivel(null).crear())
                    .criteriosCumplidos()).doesNotContain(NIVEL_ESFUERZO);
        }

        @Test
        @DisplayName("de mayor a menor afinidad, con posiciones desde 1 y como máximo 3")
        void ordenYMaximo() {
            List<OfertaCandidata> visibles = List.of(origen,
                    oferta("cero").actividades(BUCEO_SNORKEL).nivel(ALTA_EXIGENCIA).precio(300_000).crear(),
                    oferta("uno").actividades(BUCEO_SNORKEL).nivel(ALTA_EXIGENCIA).crear(),
                    oferta("tres").crear(),
                    oferta("dos").actividades(BUCEO_SNORKEL).crear());

            List<Recomendacion> alternativas = regla.proponer(origen, visibles, Idioma.ES);

            assertThat(ids(alternativas)).containsExactly("tres", "dos", "uno");
            assertThat(alternativas).extracting(Recomendacion::posicion).containsExactly(1, 2, 3);
        }

        @Test
        @DisplayName("entre empatadas: con la misma semilla, el mismo orden; con otras, cada una llega a primera")
        void empates() {
            List<OfertaCandidata> visibles = List.of(origen, oferta("a").crear(), oferta("b").crear(),
                    oferta("c").crear(), oferta("d").zona(TAGANGA, "Taganga").crear());

            assertThat(ids(new ReglaAlternativas(new PoliticaRotacion(new Random(42)))
                    .proponer(origen, visibles, Idioma.ES)))
                    .isEqualTo(ids(new ReglaAlternativas(new PoliticaRotacion(new Random(42)))
                            .proponer(origen, visibles, Idioma.ES)));
            ReglaAlternativas conAzar = new ReglaAlternativas(new PoliticaRotacion(new Random(1)));
            Set<String> primeras = new HashSet<>();
            IntStream.range(0, 200).forEach(i ->
                    primeras.add(conAzar.proponer(origen, visibles, Idioma.ES).getFirst().oferta().id()));
            assertThat(primeras).containsExactlyInAnyOrder("a", "b", "c", "d");
        }
    }

    @Nested
    @DisplayName("explicación por plantilla, sin IA")
    class Explicacion {

        @Test
        @DisplayName("en español: la zona de la alternativa y los criterios que cumple, con las actividades compartidas")
        void espanol() {
            List<OfertaCandidata> visibles = List.of(origen, oferta("minca").zona("a7", "Minca")
                    .actividades(SENDERISMO, AVISTAMIENTO_FAUNA).nivel(BAJO).crear());

            assertThat(regla.proponer(origen, visibles, Idioma.ES).getFirst().explicacion().texto()).isEqualTo(
                    "Mismo tipo de oferta en Minca, una zona con menos visitantes; también de senderismo y "
                            + "avistamiento de fauna, con esfuerzo igual o menor, precio parecido y accesible en silla de "
                            + "ruedas.");
        }

        @Test
        @DisplayName("sin actividad compartida, el \"con\" va antes del precio")
        void sinActividad() {
            List<OfertaCandidata> visibles = List.of(origen, oferta("minca").zona("a7", "Minca")
                    .actividades(BUCEO_SNORKEL).nivel(ALTA_EXIGENCIA).crear());

            assertThat(regla.proponer(origen, visibles, Idioma.ES).getFirst().explicacion().texto()).isEqualTo(
                    "Mismo tipo de oferta en Minca, una zona con menos visitantes; también con precio parecido y "
                            + "accesible en silla de ruedas.");
        }

        @Test
        @DisplayName("en inglés")
        void ingles() {
            List<OfertaCandidata> visibles = List.of(origen,
                    oferta("minca").zona("a7", "Minca").nivel(ALTA_EXIGENCIA).precio(300_000).crear());

            assertThat(regla.proponer(origen, visibles, Idioma.EN).getFirst().explicacion().texto())
                    .isEqualTo("Same type of offer in Minca, a less crowded area; also hiking and wheelchair accessible.");
        }

        @Test
        @DisplayName("sin nombre de zona ni criterios de afinidad, solo el inicio")
        void minima() {
            OfertaCandidata noAccesible = oferta("origen").zona(TAYRONA, "Parque Tayrona").altaAfluencia()
                    .accesible(false).crear();
            List<OfertaCandidata> visibles = List.of(noAccesible, oferta("x").zona("a7", null)
                    .actividades(BUCEO_SNORKEL).nivel(ALTA_EXIGENCIA).precio(300_000).crear());

            assertThat(regla.proponer(noAccesible, visibles, Idioma.ES).getFirst().explicacion().texto())
                    .isEqualTo("Mismo tipo de oferta en una zona con menos visitantes.");
        }
    }

    private static List<String> ids(List<Recomendacion> alternativas) {
        return alternativas.stream().map(r -> r.oferta().id()).toList();
    }
}
