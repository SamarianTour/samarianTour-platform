package co.edu.unimagdalena.samariantour.recomendacion.recomendacion.dominio.politica;

import co.edu.unimagdalena.samariantour.recomendacion.recomendacion.dominio.modelo.CriterioAfinidad;
import co.edu.unimagdalena.samariantour.recomendacion.recomendacion.dominio.modelo.Etiquetas;
import co.edu.unimagdalena.samariantour.recomendacion.recomendacion.dominio.modelo.Explicacion;
import co.edu.unimagdalena.samariantour.recomendacion.recomendacion.dominio.modelo.Idioma;
import co.edu.unimagdalena.samariantour.recomendacion.recomendacion.dominio.modelo.OfertaCandidata;
import co.edu.unimagdalena.samariantour.recomendacion.recomendacion.dominio.modelo.OfertaEvaluada;
import co.edu.unimagdalena.samariantour.recomendacion.recomendacion.dominio.modelo.Recomendacion;
import co.edu.unimagdalena.samariantour.recomendacion.recomendacion.dominio.modelo.ResultadoAlternativas;
import co.edu.unimagdalena.samariantour.recomendacion.recomendacion.dominio.modelo.TipoActividad;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.IntStream;

/**
 * Alternativas a una oferta de una zona con alta afluencia (RF-29, DR-18). Es una regla de sostenibilidad,
 * determinista y explicable: no usa la IA (DR-19).
 * <ul>
 *   <li>Candidatas: visibles, del mismo tipo, en zonas sin alta afluencia y distintas de la de origen. Si la de
 *       origen es accesible en silla de ruedas, también deben serlo [SUPUESTO]: no se ofrece algo menos accesible.</li>
 *   <li>Afinidad, un punto por criterio: comparte algún tipo de actividad, su esfuerzo es igual o menor y su precio
 *       queda dentro de ±50 % del de origen [SUPUESTO].</li>
 *   <li>De mayor a menor afinidad, con los empates en orden al azar ({@link PoliticaRotacion}); como máximo 3.</li>
 * </ul>
 * Los criterios de cada alternativa usan {@link CriterioAfinidad}: {@code TIPO_OFERTA} (mismo tipo) siempre,
 * {@code ACCESIBILIDAD} si la de origen es accesible, y los de afinidad que cumpla.
 */
public class ReglaAlternativas {

    /** [SUPUESTO] (DR-18): ±50 % del precio de origen. */
    static final BigDecimal MARGEN_PRECIO = new BigDecimal("0.5");

    private static final Map<Idioma, Textos> TEXTOS = Map.of(
            Idioma.ES, new Textos("Mismo tipo de oferta en %s, una zona con menos visitantes",
                    "Mismo tipo de oferta en una zona con menos visitantes", "; también ", "de %s", " y ", "con ",
                    "esfuerzo igual o menor", "precio parecido", "accesible en silla de ruedas"),
            Idioma.EN, new Textos("Same type of offer in %s, a less crowded area",
                    "Same type of offer in a less crowded area", "; also ", "%s", " and ", "with ",
                    "same or lower effort", "similar price", "wheelchair accessible"));

    private final PoliticaRotacion rotacion;

    public ReglaAlternativas(PoliticaRotacion rotacion) {
        this.rotacion = Objects.requireNonNull(rotacion, "rotacion");
    }

    /**
     * Hasta 3 alternativas, con su posición desde 1 y su explicación en el idioma pedido. No mira si la zona de
     * origen es de alta afluencia: eso lo decide el caso de uso, que responde {@code aplica: false}.
     */
    public List<Recomendacion> proponer(OfertaCandidata origen, List<OfertaCandidata> visibles, Idioma idioma) {
        List<OfertaEvaluada> evaluadas = visibles.stream()
                .filter(candidata -> esCandidata(origen, candidata))
                .map(candidata -> evaluar(origen, candidata))
                .toList();
        List<OfertaEvaluada> elegidas = rotacion.ordenar(evaluadas).stream()
                .limit(ResultadoAlternativas.MAXIMO)
                .toList();
        return IntStream.range(0, elegidas.size())
                .mapToObj(i -> new Recomendacion(i + 1, elegidas.get(i).oferta(),
                        explicar(origen, elegidas.get(i), idioma)))
                .toList();
    }

    static boolean esCandidata(OfertaCandidata origen, OfertaCandidata candidata) {
        return candidata.tipo() == origen.tipo()
                && !candidata.zonaAltaAfluencia()
                && !candidata.id().equals(origen.id())
                && (!origen.accesibleSillaRuedas() || candidata.accesibleSillaRuedas());
    }

    static OfertaEvaluada evaluar(OfertaCandidata origen, OfertaCandidata candidata) {
        Set<CriterioAfinidad> criterios = EnumSet.of(CriterioAfinidad.TIPO_OFERTA);
        if (origen.accesibleSillaRuedas()) {
            criterios.add(CriterioAfinidad.ACCESIBILIDAD);
        }
        int afinidad = 0;
        if (!actividadesCompartidas(origen, candidata).isEmpty()) {
            criterios.add(CriterioAfinidad.TIPO_ACTIVIDAD);
            afinidad++;
        }
        if (origen.nivelEsfuerzo() != null && candidata.nivelEsfuerzo() != null
                && candidata.nivelEsfuerzo().noSupera(origen.nivelEsfuerzo())) {
            criterios.add(CriterioAfinidad.NIVEL_ESFUERZO);
            afinidad++;
        }
        if (precioParecido(origen.precioReferencia(), candidata.precioReferencia())) {
            criterios.add(CriterioAfinidad.PRECIO);
            afinidad++;
        }
        return new OfertaEvaluada(candidata, criterios, afinidad);
    }

    /** {@code |precio − origen| ≤ 50 % del de origen}; con origen gratis, solo otra gratis. */
    static boolean precioParecido(BigDecimal origen, BigDecimal precio) {
        return precio.subtract(origen).abs().compareTo(origen.multiply(MARGEN_PRECIO)) <= 0;
    }

    private static Set<TipoActividad> actividadesCompartidas(OfertaCandidata origen, OfertaCandidata candidata) {
        Set<TipoActividad> compartidas = EnumSet.noneOf(TipoActividad.class);
        compartidas.addAll(candidata.tiposActividad());
        compartidas.retainAll(origen.tiposActividad());
        return compartidas;
    }

    /**
     * Por plantilla, sin IA: "Mismo tipo de oferta en Minca, una zona con menos visitantes; también de avistamiento de
     * fauna, con esfuerzo igual o menor y accesible en silla de ruedas."
     */
    private static Explicacion explicar(OfertaCandidata origen, OfertaEvaluada evaluada, Idioma idioma) {
        Textos textos = TEXTOS.get(idioma);
        OfertaCandidata oferta = evaluada.oferta();
        Set<CriterioAfinidad> cumplidos = evaluada.criteriosCumplidos();
        List<String> partes = new ArrayList<>();
        if (cumplidos.contains(CriterioAfinidad.TIPO_ACTIVIDAD)) {
            partes.add(textos.actividad().formatted(unir(actividadesCompartidas(origen, oferta).stream()
                    .map(tipo -> Etiquetas.de(tipo, idioma)).toList(), textos.y())));
        }
        // "con" solo antes del primero de los dos: "con esfuerzo igual o menor, precio parecido".
        int antesDeEsfuerzoYPrecio = partes.size();
        if (cumplidos.contains(CriterioAfinidad.NIVEL_ESFUERZO)) {
            partes.add(textos.esfuerzo());
        }
        if (cumplidos.contains(CriterioAfinidad.PRECIO)) {
            partes.add(textos.precio());
        }
        if (partes.size() > antesDeEsfuerzoYPrecio) {
            partes.set(antesDeEsfuerzoYPrecio, textos.con() + partes.get(antesDeEsfuerzoYPrecio));
        }
        if (cumplidos.contains(CriterioAfinidad.ACCESIBILIDAD)) {
            partes.add(textos.accesible());
        }
        String texto = (oferta.zonaNombre() == null
                ? textos.inicioSinZona()
                : textos.inicio().formatted(oferta.zonaNombre()))
                + (partes.isEmpty() ? "" : textos.tambien() + unir(partes, textos.y())) + ".";
        return new Explicacion(List.copyOf(cumplidos), texto.length() <= Explicacion.TEXTO_MAXIMO
                ? texto
                : texto.substring(0, Explicacion.TEXTO_MAXIMO - 1) + "…");
    }

    /** "a", "a y b", "a, b y c". */
    private static String unir(List<String> partes, String y) {
        if (partes.size() <= 1) {
            return String.join("", partes);
        }
        return String.join(", ", partes.subList(0, partes.size() - 1)) + y + partes.getLast();
    }

    private record Textos(String inicio, String inicioSinZona, String tambien, String actividad, String y, String con,
                          String esfuerzo, String precio, String accesible) {
    }
}
