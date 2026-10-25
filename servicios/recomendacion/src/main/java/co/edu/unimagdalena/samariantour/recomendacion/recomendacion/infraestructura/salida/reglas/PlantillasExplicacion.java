package co.edu.unimagdalena.samariantour.recomendacion.recomendacion.infraestructura.salida.reglas;

import co.edu.unimagdalena.samariantour.recomendacion.recomendacion.dominio.modelo.CriterioAfinidad;
import co.edu.unimagdalena.samariantour.recomendacion.recomendacion.dominio.modelo.Etiquetas;
import co.edu.unimagdalena.samariantour.recomendacion.recomendacion.dominio.modelo.Explicacion;
import co.edu.unimagdalena.samariantour.recomendacion.recomendacion.dominio.modelo.Idioma;
import co.edu.unimagdalena.samariantour.recomendacion.recomendacion.dominio.modelo.OfertaCandidata;
import co.edu.unimagdalena.samariantour.recomendacion.recomendacion.dominio.modelo.OfertaEvaluada;
import co.edu.unimagdalena.samariantour.recomendacion.recomendacion.dominio.modelo.Preferencias;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Explicaciones del motor por reglas, en español e inglés, armadas solo con los criterios que la oferta cumple, por
 * ejemplo: "Coincide con: senderismo, zona Minca, accesible en silla de ruedas, precio dentro de tu presupuesto."
 */
class PlantillasExplicacion {

    private static final Map<Idioma, Textos> TEXTOS = new EnumMap<>(Map.of(
            Idioma.ES, new Textos("Coincide con: ", "Relacionada con lo que escribiste: ",
                    "Sin coincidencias con tus filtros: es una de las ofertas visibles del catálogo, mostrada en "
                            + "orden aleatorio.",
                    "zona %s", "la zona que elegiste", "accesible en silla de ruedas", "esfuerzo %s",
                    "precio dentro de tu presupuesto"),
            Idioma.EN, new Textos("Matches: ", "Related to what you wrote: ",
                    "No match with your filters: it is one of the catalog's visible offers, shown in random order.",
                    "%s area", "the area you chose", "wheelchair accessible", "%s effort",
                    "price within your budget")));

    /**
     * El texto para el turista, en su idioma: los criterios que cumple, en el orden del enum, y las palabras de su
     * texto que coinciden con la oferta (DR-13).
     */
    String explicar(OfertaEvaluada evaluada, Preferencias preferencias) {
        Idioma idioma = preferencias.idioma();
        Textos textos = TEXTOS.get(idioma);
        if (evaluada.criteriosCumplidos().isEmpty() && evaluada.palabrasDelTexto().isEmpty()) {
            return textos.sinCoincidencias();
        }
        List<String> frases = new ArrayList<>();
        if (!evaluada.criteriosCumplidos().isEmpty()) {
            frases.add(textos.inicio() + String.join(", ", criterios(evaluada, preferencias, textos, idioma)) + ".");
        }
        if (!evaluada.palabrasDelTexto().isEmpty()) {
            frases.add(textos.texto() + String.join(", ", evaluada.palabrasDelTexto()) + ".");
        }
        String texto = String.join(" ", frases);
        // Con los 12 tipos de actividad o un texto muy largo no alcanza; en la práctica casi nunca se recorta.
        return texto.length() <= Explicacion.TEXTO_MAXIMO
                ? texto
                : texto.substring(0, Explicacion.TEXTO_MAXIMO - 1) + "…";
    }

    private static List<String> criterios(OfertaEvaluada evaluada, Preferencias preferencias, Textos textos,
                                          Idioma idioma) {
        OfertaCandidata oferta = evaluada.oferta();
        List<String> partes = new ArrayList<>();
        for (CriterioAfinidad criterio : evaluada.criteriosCumplidos()) {
            partes.add(switch (criterio) {
                case TIPO_OFERTA -> Etiquetas.de(oferta.tipo(), idioma);
                case TIPO_ACTIVIDAD -> oferta.tiposActividad().stream()
                        .filter(preferencias.tiposActividad()::contains)
                        .map(tipo -> Etiquetas.de(tipo, idioma))
                        .collect(Collectors.joining(", "));
                case ZONA -> oferta.zonaNombre() == null
                        ? textos.zonaSinNombre()
                        : textos.zona().formatted(oferta.zonaNombre());
                case ACCESIBILIDAD -> textos.accesible();
                case NIVEL_ESFUERZO -> textos.esfuerzo().formatted(Etiquetas.de(oferta.nivelEsfuerzo(), idioma));
                case PRECIO -> textos.precio();
            });
        }
        return partes;
    }

    private record Textos(String inicio, String texto, String sinCoincidencias, String zona, String zonaSinNombre,
                          String accesible, String esfuerzo, String precio) {
    }
}
