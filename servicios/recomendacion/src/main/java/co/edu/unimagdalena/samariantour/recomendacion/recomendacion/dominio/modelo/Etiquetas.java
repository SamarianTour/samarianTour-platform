package co.edu.unimagdalena.samariantour.recomendacion.recomendacion.dominio.modelo;

import java.util.EnumMap;
import java.util.Map;

/**
 * Cómo se nombran ante el turista, en español e inglés, los tipos de oferta, los tipos de actividad y los niveles de
 * esfuerzo. Las comparten las explicaciones de la recomendación y las de las alternativas (RF-27, RF-29).
 */
public final class Etiquetas {

    private static final Map<Idioma, Map<TipoOferta, String>> TIPOS_OFERTA = new EnumMap<>(Map.of(
            Idioma.ES, Map.of(TipoOferta.ACTIVIDAD, "actividad", TipoOferta.PAQUETE, "paquete",
                    TipoOferta.ALOJAMIENTO, "alojamiento", TipoOferta.RESTAURANTE, "restaurante"),
            Idioma.EN, Map.of(TipoOferta.ACTIVIDAD, "activity", TipoOferta.PAQUETE, "package",
                    TipoOferta.ALOJAMIENTO, "lodging", TipoOferta.RESTAURANTE, "restaurant")));

    private static final Map<Idioma, Map<TipoActividad, String>> TIPOS_ACTIVIDAD = new EnumMap<>(Map.of(
            Idioma.ES, Map.ofEntries(
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
            Idioma.EN, Map.ofEntries(
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
                    Map.entry(TipoActividad.OTRO, "other activities"))));

    private static final Map<Idioma, Map<NivelEsfuerzo, String>> NIVELES = new EnumMap<>(Map.of(
            Idioma.ES, Map.of(NivelEsfuerzo.BAJO, "bajo", NivelEsfuerzo.MODERADO, "moderado",
                    NivelEsfuerzo.ALTA_EXIGENCIA, "de alta exigencia"),
            Idioma.EN, Map.of(NivelEsfuerzo.BAJO, "low", NivelEsfuerzo.MODERADO, "moderate",
                    NivelEsfuerzo.ALTA_EXIGENCIA, "high")));

    private Etiquetas() {
    }

    public static String de(TipoOferta tipo, Idioma idioma) {
        return TIPOS_OFERTA.get(idioma).get(tipo);
    }

    public static String de(TipoActividad tipo, Idioma idioma) {
        return TIPOS_ACTIVIDAD.get(idioma).get(tipo);
    }

    public static String de(NivelEsfuerzo nivel, Idioma idioma) {
        return NIVELES.get(idioma).get(nivel);
    }
}
