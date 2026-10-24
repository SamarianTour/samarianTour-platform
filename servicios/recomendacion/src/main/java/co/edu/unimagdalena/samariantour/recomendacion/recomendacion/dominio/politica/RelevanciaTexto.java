package co.edu.unimagdalena.samariantour.recomendacion.recomendacion.dominio.politica;

import co.edu.unimagdalena.samariantour.recomendacion.recomendacion.dominio.modelo.OfertaCandidata;
import co.edu.unimagdalena.samariantour.recomendacion.recomendacion.dominio.modelo.TipoActividad;
import co.edu.unimagdalena.samariantour.recomendacion.recomendacion.dominio.modelo.TipoOferta;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Qué palabras del texto del turista coinciden con una oferta (DR-13): su nombre, su descripción, su zona y sus tipos,
 * cada tipo con unas palabras con que la gente suele nombrarlo ("aves" para avistamiento de fauna, "comer" para
 * restaurante). Sin mayúsculas ni tildes; "snorkeling" coincide con "snorkel" porque una empieza por la otra.
 * <p>Solo usa el contenido de la oferta, que el administrador moderó (RF-79): nunca nada del prestador.
 */
public class RelevanciaTexto {

    /** Una palabra más corta no distingue nada ("mar" sí; "el" no). */
    private static final int LARGO_MINIMO = 3;
    /** Para coincidir por el comienzo, las dos palabras tienen al menos este largo ("cafe" y "cafetal"). */
    private static final int LARGO_PREFIJO = 4;

    private static final Set<String> VACIAS = Set.of(
            "algo", "cerca", "lugar", "sitio", "zona", "como", "con", "cual", "del", "desde", "donde", "esta", "este", "estoy", "hacer", "las", "los",
            "mas", "mis", "mucho", "muy", "nos", "nuestro", "para", "pero", "poco", "por", "que", "queremos",
            "quiero", "sea", "ser", "sin", "son", "sus", "tal", "tener", "todo", "una", "uno", "unos", "unas", "ver",
            "viajo", "viajamos", "vamos", "busco", "buscamos", "gusta", "gustaria", "solo", "bien", "dia", "dias",
            "and", "the", "for", "with", "want", "would", "like", "some", "something", "near", "very", "looking",
            "our", "are", "can", "get", "but", "from", "that", "this");

    private static final Map<TipoActividad, Set<String>> PALABRAS_ACTIVIDAD = Map.ofEntries(
            Map.entry(TipoActividad.SENDERISMO,
                    Set.of("senderismo", "sendero", "caminata", "caminar", "trekking", "hiking", "hike", "trail")),
            Map.entry(TipoActividad.BUCEO_SNORKEL,
                    Set.of("buceo", "bucear", "snorkel", "careteo", "arrecife", "diving", "dive", "reef")),
            Map.entry(TipoActividad.DEPORTES_ACUATICOS,
                    Set.of("kayak", "paddle", "surf", "acuatico", "acuaticos", "water")),
            Map.entry(TipoActividad.NAUTICO,
                    Set.of("velero", "lancha", "bote", "barco", "navegar", "nautico", "sailing", "boat")),
            Map.entry(TipoActividad.AVISTAMIENTO_FAUNA,
                    Set.of("aves", "pajaros", "fauna", "avistamiento", "animales", "birds", "birdwatching",
                            "wildlife")),
            Map.entry(TipoActividad.CULTURAL_HISTORICO,
                    Set.of("historia", "historico", "cultura", "cultural", "museo", "colonial", "history",
                            "culture")),
            Map.entry(TipoActividad.ETNOTURISMO,
                    Set.of("etnoturismo", "indigena", "indigenas", "comunidad", "kogui", "arhuaco", "indigenous")),
            Map.entry(TipoActividad.GASTRONOMICO,
                    Set.of("gastronomia", "gastronomico", "cocina", "comida", "comer", "cafe", "cacao", "food",
                            "cooking", "coffee")),
            Map.entry(TipoActividad.CICLISMO, Set.of("ciclismo", "bici", "bicicleta", "cycling", "bike")),
            Map.entry(TipoActividad.AVENTURA, Set.of("aventura", "adrenalina", "extremo", "adventure")),
            Map.entry(TipoActividad.PLAYA_RELAX,
                    Set.of("playa", "playas", "descansar", "descanso", "relax", "relajarse", "beach")),
            Map.entry(TipoActividad.OTRO, Set.of()));

    private static final Map<TipoOferta, Set<String>> PALABRAS_TIPO = Map.of(
            TipoOferta.ACTIVIDAD, Set.of("actividad", "tour", "activity"),
            TipoOferta.PAQUETE, Set.of("paquete", "package"),
            TipoOferta.ALOJAMIENTO, Set.of("alojamiento", "hotel", "hospedaje", "dormir", "habitacion", "cabana",
                    "apartamento", "lodging", "stay", "room"),
            TipoOferta.RESTAURANTE, Set.of("restaurante", "comer", "comida", "almorzar", "cenar", "restaurant",
                    "food", "eat", "dinner", "lunch"));

    /**
     * Las palabras del texto que coinciden con la oferta, como las escribió el turista (en minúsculas) y sin
     * repetir, en el orden del texto. Vacía si no hay texto.
     */
    public List<String> coincidencias(String texto, OfertaCandidata oferta) {
        if (texto == null || texto.isBlank()) {
            return List.of();
        }
        Set<String> deLaOferta = palabrasDe(oferta);
        List<String> coincidentes = new ArrayList<>();
        for (Map.Entry<String, String> palabra : palabrasDelTexto(texto).entrySet()) {
            if (deLaOferta.stream().anyMatch(otra -> coinciden(palabra.getKey(), otra))) {
                coincidentes.add(palabra.getValue());
            }
        }
        return List.copyOf(coincidentes);
    }

    /** Normalizada → como la escribió el turista, sin las vacías ni las cortas. */
    private static Map<String, String> palabrasDelTexto(String texto) {
        Map<String, String> palabras = new LinkedHashMap<>();
        for (String original : texto.toLowerCase(Locale.ROOT).split("[^\\p{L}\\p{N}]+")) {
            String normalizada = normalizar(original);
            if (normalizada.length() >= LARGO_MINIMO && !VACIAS.contains(normalizada)) {
                palabras.putIfAbsent(normalizada, original);
            }
        }
        return palabras;
    }

    private static Set<String> palabrasDe(OfertaCandidata oferta) {
        Set<String> palabras = new HashSet<>();
        for (String texto : new String[] {oferta.nombre(), oferta.descripcion(), oferta.zonaNombre()}) {
            if (texto != null) {
                for (String palabra : texto.split("[^\\p{L}\\p{N}]+")) {
                    String normalizada = normalizar(palabra);
                    if (normalizada.length() >= LARGO_MINIMO) {
                        palabras.add(normalizada);
                    }
                }
            }
        }
        palabras.addAll(PALABRAS_TIPO.get(oferta.tipo()));
        oferta.tiposActividad().forEach(tipo -> palabras.addAll(PALABRAS_ACTIVIDAD.get(tipo)));
        return palabras;
    }

    private static boolean coinciden(String delTexto, String deLaOferta) {
        if (delTexto.equals(deLaOferta)) {
            return true;
        }
        return delTexto.length() >= LARGO_PREFIJO && deLaOferta.length() >= LARGO_PREFIJO
                && (delTexto.startsWith(deLaOferta) || deLaOferta.startsWith(delTexto));
    }

    /** Minúsculas y sin tildes: "Montaña" es "montana". */
    static String normalizar(String palabra) {
        return Normalizer.normalize(palabra.toLowerCase(Locale.ROOT), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
    }
}
