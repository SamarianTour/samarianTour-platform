package co.edu.unimagdalena.samariantour.recomendacion.recomendacion.infraestructura.salida.reglas;

import co.edu.unimagdalena.samariantour.recomendacion.recomendacion.dominio.modelo.CriterioAfinidad;
import co.edu.unimagdalena.samariantour.recomendacion.recomendacion.dominio.modelo.Explicacion;
import co.edu.unimagdalena.samariantour.recomendacion.recomendacion.dominio.modelo.Idioma;
import co.edu.unimagdalena.samariantour.recomendacion.recomendacion.dominio.modelo.NivelEsfuerzo;
import co.edu.unimagdalena.samariantour.recomendacion.recomendacion.dominio.modelo.OfertaCandidata;
import co.edu.unimagdalena.samariantour.recomendacion.recomendacion.dominio.modelo.OfertaEvaluada;
import co.edu.unimagdalena.samariantour.recomendacion.recomendacion.dominio.modelo.Preferencias;
import co.edu.unimagdalena.samariantour.recomendacion.recomendacion.dominio.modelo.TipoActividad;
import co.edu.unimagdalena.samariantour.recomendacion.recomendacion.dominio.modelo.TipoOferta;

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
            Idioma.ES, new Textos("Coincide con: ",
                    "Sin coincidencias con tus filtros: es una de las ofertas visibles del catálogo, mostrada en "
                            + "orden aleatorio.",
                    "zona %s", "la zona que elegiste", "accesible en silla de ruedas", "esfuerzo %s",
                    "precio dentro de tu presupuesto",
                    Map.of(TipoOferta.ACTIVIDAD, "actividad", TipoOferta.PAQUETE, "paquete",
                            TipoOferta.ALOJAMIENTO, "alojamiento", TipoOferta.RESTAURANTE, "restaurante"),
                    Map.ofEntries(
                            Map.entry(TipoActividad.SENDERISMO, "senderismo"),
                            Map.entry(TipoActividad.BUCEO_SNORKEL, "buceo y snorkel"),
                            Map.entry(TipoActividad.DEPORTES_ACUATICOS, "deportes acuáticos"),
                            Map.entry(TipoActividad.NAUTICO, "náutico"),
                            Map.entry(TipoActividad.AVISTAMIENTO_FAUNA, "avistamiento de fauna"),
                            Map.entry(TipoActividad.CULTURAL_HISTORICO, "cultural e histórico"),
                            Map.entry(TipoActividad.ETNOTURISMO, "etnoturismo"),
                            Map.entry(TipoActividad.GASTRONOMICO, "gastronómico"),
                            Map.entry(TipoActividad.CICLISMO, "ciclismo"),
                            Map.entry(TipoActividad.AVENTURA, "aventura"),
                            Map.entry(TipoActividad.PLAYA_RELAX, "playa y descanso"),
                            Map.entry(TipoActividad.OTRO, "otras actividades")),
                    Map.of(NivelEsfuerzo.BAJO, "bajo", NivelEsfuerzo.MODERADO, "moderado",
                            NivelEsfuerzo.ALTA_EXIGENCIA, "de alta exigencia")),
            Idioma.EN, new Textos("Matches: ",
                    "No match with your filters: it is one of the catalog's visible offers, shown in random order.",
                    "%s area", "the area you chose", "wheelchair accessible", "%s effort",
                    "price within your budget",
                    Map.of(TipoOferta.ACTIVIDAD, "activity", TipoOferta.PAQUETE, "package",
                            TipoOferta.ALOJAMIENTO, "lodging", TipoOferta.RESTAURANTE, "restaurant"),
                    Map.ofEntries(
                            Map.entry(TipoActividad.SENDERISMO, "hiking"),
                            Map.entry(TipoActividad.BUCEO_SNORKEL, "diving and snorkeling"),
                            Map.entry(TipoActividad.DEPORTES_ACUATICOS, "water sports"),
                            Map.entry(TipoActividad.NAUTICO, "boating"),
                            Map.entry(TipoActividad.AVISTAMIENTO_FAUNA, "wildlife watching"),
                            Map.entry(TipoActividad.CULTURAL_HISTORICO, "culture and history"),
                            Map.entry(TipoActividad.ETNOTURISMO, "ethnotourism"),
                            Map.entry(TipoActividad.GASTRONOMICO, "food"),
                            Map.entry(TipoActividad.CICLISMO, "cycling"),
                            Map.entry(TipoActividad.AVENTURA, "adventure"),
                            Map.entry(TipoActividad.PLAYA_RELAX, "beach and relaxation"),
                            Map.entry(TipoActividad.OTRO, "other activities")),
                    Map.of(NivelEsfuerzo.BAJO, "low", NivelEsfuerzo.MODERADO, "moderate",
                            NivelEsfuerzo.ALTA_EXIGENCIA, "high"))));

    /** El texto para el turista, en su idioma y con los criterios en el orden del enum. */
    String explicar(OfertaEvaluada evaluada, Preferencias preferencias) {
        Textos textos = TEXTOS.get(preferencias.idioma());
        if (evaluada.criteriosCumplidos().isEmpty()) {
            return textos.sinCoincidencias();
        }
        OfertaCandidata oferta = evaluada.oferta();
        List<String> partes = new ArrayList<>();
        for (CriterioAfinidad criterio : evaluada.criteriosCumplidos()) {
            partes.add(switch (criterio) {
                case TIPO_OFERTA -> textos.tiposOferta().get(oferta.tipo());
                case TIPO_ACTIVIDAD -> oferta.tiposActividad().stream()
                        .filter(preferencias.tiposActividad()::contains)
                        .map(textos.tiposActividad()::get)
                        .collect(Collectors.joining(", "));
                case ZONA -> oferta.zonaNombre() == null
                        ? textos.zonaSinNombre()
                        : textos.zona().formatted(oferta.zonaNombre());
                case ACCESIBILIDAD -> textos.accesible();
                case NIVEL_ESFUERZO -> textos.esfuerzo().formatted(textos.niveles().get(oferta.nivelEsfuerzo()));
                case PRECIO -> textos.precio();
            });
        }
        String texto = textos.inicio() + String.join(", ", partes) + ".";
        // Con los 12 tipos de actividad a la vez no alcanza; en la práctica nunca se recorta.
        return texto.length() <= Explicacion.TEXTO_MAXIMO
                ? texto
                : texto.substring(0, Explicacion.TEXTO_MAXIMO - 1) + "…";
    }

    private record Textos(String inicio, String sinCoincidencias, String zona, String zonaSinNombre,
                          String accesible, String esfuerzo, String precio, Map<TipoOferta, String> tiposOferta,
                          Map<TipoActividad, String> tiposActividad, Map<NivelEsfuerzo, String> niveles) {
    }
}
