package co.edu.unimagdalena.samariantour.recomendacion.recomendacion.dominio.politica;

import co.edu.unimagdalena.samariantour.recomendacion.recomendacion.dominio.excepcion.RespuestaIAInvalidaException;
import co.edu.unimagdalena.samariantour.recomendacion.recomendacion.dominio.modelo.OfertaEvaluada;
import co.edu.unimagdalena.samariantour.recomendacion.recomendacion.dominio.modelo.Preferencias;
import co.edu.unimagdalena.samariantour.recomendacion.recomendacion.dominio.modelo.PropuestaIA;
import co.edu.unimagdalena.samariantour.recomendacion.recomendacion.dominio.modelo.Recomendacion;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.IntStream;

import static co.edu.unimagdalena.samariantour.recomendacion.recomendacion.OfertasDePrueba.oferta;
import static co.edu.unimagdalena.samariantour.recomendacion.recomendacion.OfertasDePrueba.preferencias;
import static co.edu.unimagdalena.samariantour.recomendacion.recomendacion.dominio.modelo.CriterioAfinidad.PRECIO;
import static co.edu.unimagdalena.samariantour.recomendacion.recomendacion.dominio.modelo.CriterioAfinidad.TIPO_ACTIVIDAD;
import static co.edu.unimagdalena.samariantour.recomendacion.recomendacion.dominio.modelo.TipoActividad.SENDERISMO;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ValidacionRespuestaIATest {

    private final ReglaCriteriosExplicitos regla = new ReglaCriteriosExplicitos();
    private final ValidacionRespuestaIA validacion = new ValidacionRespuestaIA(regla);

    /** Senderismo y precio máximo de 100.000; la oferta "cara" pasa de ese precio solo para la prueba del criterio. */
    private final Preferencias preferencias = preferencias().actividades(SENDERISMO).precioMaximo(100_000).crear();
    private final List<OfertaEvaluada> listaCorta = regla.evaluar(
            IntStream.rangeClosed(1, 6).mapToObj(i -> oferta("o" + i).crear()).toList(), preferencias);

    @Test
    @DisplayName("una propuesta válida se acepta completa, con las posiciones en el orden de la IA")
    void valida() {
        List<Recomendacion> recomendaciones = validacion.validar(propuesta(
                item("o3", "TIPO_ACTIVIDAD", "PRECIO"), item("o1", "TIPO_ACTIVIDAD"), item("o5", "PRECIO")),
                listaCorta, preferencias);
        assertThat(recomendaciones).extracting(r -> r.oferta().id()).containsExactly("o3", "o1", "o5");
        assertThat(recomendaciones).extracting(Recomendacion::posicion).containsExactly(1, 2, 3);
        assertThat(recomendaciones.getFirst().explicacion().criterios()).containsExactly(TIPO_ACTIVIDAD, PRECIO);
    }

    @Test
    @DisplayName("el JSON sin la lista de recomendaciones se descarta")
    void sinLista() {
        assertInvalida(new PropuestaIA(null), "no trae la lista");
        assertThatThrownBy(() -> validacion.validar(null, listaCorta, preferencias))
                .isInstanceOf(RespuestaIAInvalidaException.class);
    }

    @Test
    @DisplayName("menos de 3 o más de 5 ofertas se descarta")
    void cantidad() {
        assertInvalida(propuesta(item("o1", "PRECIO"), item("o2", "PRECIO")), "entre 3 y 5");
        assertInvalida(propuesta(IntStream.rangeClosed(1, 6).mapToObj(i -> item("o" + i, "PRECIO"))
                .toArray(PropuestaIA.Item[]::new)), "entre 3 y 5");
    }

    @Test
    @DisplayName("con una lista corta de 2, la IA tiene que devolver las 2")
    void listaCortaPequena() {
        List<OfertaEvaluada> dos = listaCorta.subList(0, 2);
        assertThat(validacion.validar(propuesta(item("o2", "PRECIO"), item("o1", "PRECIO")), dos, preferencias))
                .hasSize(2);
        assertThatThrownBy(() -> validacion.validar(propuesta(item("o1", "PRECIO")), dos, preferencias))
                .isInstanceOf(RespuestaIAInvalidaException.class);
    }

    @Test
    @DisplayName("un id inventado, que no está en la lista corta, descarta toda la respuesta")
    void idInventado() {
        assertInvalida(propuesta(item("o1", "PRECIO"), item("inventada", "PRECIO"), item("o2", "PRECIO")),
                "no está en la lista");
    }

    @Test
    @DisplayName("un id repetido se descarta")
    void idRepetido() {
        assertInvalida(propuesta(item("o1", "PRECIO"), item("o2", "PRECIO"), item("o1", "TIPO_ACTIVIDAD")),
                "dos veces");
    }

    @Test
    @DisplayName("un criterio falso para esa oferta se descarta: dice ACCESIBILIDAD y no es accesible")
    void criterioFalso() {
        List<OfertaEvaluada> conUnaNoAccesible = new ArrayList<>(listaCorta);
        conUnaNoAccesible.set(0, regla.evaluar(List.of(oferta("o1").accesible(false).crear()), preferencias)
                .getFirst());
        assertThatThrownBy(() -> validacion.validar(propuesta(item("o1", "ACCESIBILIDAD"), item("o2", "PRECIO"),
                item("o3", "PRECIO")), conUnaNoAccesible, preferencias))
                .isInstanceOf(RespuestaIAInvalidaException.class).hasMessageContaining("ACCESIBILIDAD no es cierto");
        assertInvalida(propuesta(item("o1", "ZONA"), item("o2", "PRECIO"), item("o3", "PRECIO")),
                "ZONA no es cierto");
    }

    @Test
    @DisplayName("un criterio que no existe en el enum, o ninguno, se descarta")
    void criterioDesconocido() {
        assertInvalida(propuesta(item("o1", "CALIFICACION"), item("o2", "PRECIO"), item("o3", "PRECIO")),
                "no existe");
        assertInvalida(propuesta(item("o1"), item("o2", "PRECIO"), item("o3", "PRECIO")), "ningún criterio");
    }

    @Test
    @DisplayName("una explicación de más de 300 caracteres, o vacía, se descarta")
    void explicacion() {
        assertInvalida(propuesta(new PropuestaIA.Item("o1", List.of("PRECIO"), "x".repeat(301)),
                item("o2", "PRECIO"), item("o3", "PRECIO")), "300 caracteres");
        assertInvalida(propuesta(new PropuestaIA.Item("o1", List.of("PRECIO"), "  "),
                item("o2", "PRECIO"), item("o3", "PRECIO")), "vacía");
    }

    private void assertInvalida(PropuestaIA propuesta, String motivo) {
        assertThatThrownBy(() -> validacion.validar(propuesta, listaCorta, preferencias))
                .isInstanceOf(RespuestaIAInvalidaException.class).hasMessageContaining(motivo);
    }

    private static PropuestaIA propuesta(PropuestaIA.Item... items) {
        return new PropuestaIA(Arrays.asList(items));
    }

    private static PropuestaIA.Item item(String ofertaId, String... criterios) {
        return new PropuestaIA.Item(ofertaId, List.of(criterios), "Coincide con lo que buscas.");
    }
}
