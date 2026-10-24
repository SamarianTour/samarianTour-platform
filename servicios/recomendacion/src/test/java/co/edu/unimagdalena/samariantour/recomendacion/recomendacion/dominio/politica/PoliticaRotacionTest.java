package co.edu.unimagdalena.samariantour.recomendacion.recomendacion.dominio.politica;

import co.edu.unimagdalena.samariantour.recomendacion.recomendacion.dominio.modelo.CriterioAfinidad;
import co.edu.unimagdalena.samariantour.recomendacion.recomendacion.dominio.modelo.OfertaEvaluada;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.stream.IntStream;

import static co.edu.unimagdalena.samariantour.recomendacion.recomendacion.OfertasDePrueba.oferta;
import static org.assertj.core.api.Assertions.assertThat;

class PoliticaRotacionTest {

    /** Cuatro ofertas empatadas en afinidad 1, una con 2 y una con 0, en el orden del catálogo. */
    private static final List<OfertaEvaluada> EVALUADAS = List.of(
            evaluada("cero", 0), evaluada("a", 1), evaluada("b", 1), evaluada("dos", 2), evaluada("c", 1),
            evaluada("d", 1));

    @Test
    @DisplayName("siempre de mayor a menor afinidad")
    void ordenPorAfinidad() {
        List<OfertaEvaluada> ordenadas = new PoliticaRotacion(new Random(7)).ordenar(EVALUADAS);
        assertThat(ordenadas).extracting(OfertaEvaluada::afinidad).containsExactly(2, 1, 1, 1, 1, 0);
        assertThat(ordenadas.getFirst().oferta().id()).isEqualTo("dos");
        assertThat(ordenadas.getLast().oferta().id()).isEqualTo("cero");
    }

    @Test
    @DisplayName("con la misma afinidad, desempatan las palabras del texto que coinciden; el azar, solo después")
    void desempatePorTexto() {
        List<OfertaEvaluada> evaluadas = List.of(
                new OfertaEvaluada(oferta("sin").crear(), Set.of(), 0),
                new OfertaEvaluada(oferta("una").crear(), Set.of(), 0, List.of("snorkel")),
                new OfertaEvaluada(oferta("dos").crear(), Set.of(), 0, List.of("snorkel", "playa")),
                new OfertaEvaluada(oferta("afin").crear(), Set.of(), 1));

        for (long semilla = 0; semilla < 20; semilla++) {
            assertThat(ids(new PoliticaRotacion(new Random(semilla)).ordenar(evaluadas)))
                    .as("semilla %d", semilla).containsExactly("afin", "dos", "una", "sin");
        }
    }

    @Test
    @DisplayName("con la misma semilla, el mismo orden: las pruebas son deterministas")
    void determinista() {
        assertThat(ids(new PoliticaRotacion(new Random(42)).ordenar(EVALUADAS)))
                .isEqualTo(ids(new PoliticaRotacion(new Random(42)).ordenar(EVALUADAS)));
    }

    @Test
    @DisplayName("entre empatadas el orden rota: cada una de las cuatro llega a quedar de segunda")
    void rotaEntreEmpates() {
        PoliticaRotacion rotacion = new PoliticaRotacion(new Random(1));
        Set<String> segundas = new HashSet<>();
        IntStream.range(0, 200).forEach(i -> segundas.add(rotacion.ordenar(EVALUADAS).get(1).oferta().id()));
        assertThat(segundas).containsExactlyInAnyOrder("a", "b", "c", "d");
    }

    @Test
    @DisplayName("barajar devuelve una copia con los mismos elementos y no cambia la lista original")
    void barajar() {
        List<String> original = List.of("1", "2", "3", "4", "5");
        List<String> barajada = new PoliticaRotacion(new Random(3)).barajar(original);
        assertThat(barajada).containsExactlyInAnyOrderElementsOf(original);
        assertThat(original).containsExactly("1", "2", "3", "4", "5");
    }

    private static OfertaEvaluada evaluada(String id, int afinidad) {
        return new OfertaEvaluada(oferta(id).crear(), Set.<CriterioAfinidad>of(), afinidad);
    }

    private static List<String> ids(List<OfertaEvaluada> evaluadas) {
        return evaluadas.stream().map(e -> e.oferta().id()).toList();
    }
}
