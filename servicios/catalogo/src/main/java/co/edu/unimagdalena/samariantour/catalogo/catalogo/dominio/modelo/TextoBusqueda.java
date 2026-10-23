package co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo;

import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.excepcion.DatoInvalidoException;

import java.text.Normalizer;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Texto libre de la búsqueda (RF-14, DC-37). Una oferta coincide si cada palabra del texto aparece en su nombre o en
 * su descripción, aunque sea incompleta ("buce" encuentra "buceo"), sin distinguir mayúsculas ni tildes.
 *
 * <p>Las palabras se comparan con expresiones regulares que arma el dominio: el adaptador en memoria las evalúa con
 * {@code java.util.regex} y el de MongoDB las envía como {@code $regex}, así que los dos dan los mismos resultados.
 *
 * @param palabras palabras del texto, normalizadas: sin tildes y en minúsculas
 */
public record TextoBusqueda(List<String> palabras) {

    public static final int LONGITUD_MINIMA = 2;
    public static final int LONGITUD_MAXIMA = 100;

    private static final Pattern MARCAS = Pattern.compile("\\p{M}+");
    private static final Pattern ESPACIOS = Pattern.compile("\\s+");
    private static final String ESPECIALES = "\\^$.|?*+()[]{}-/";

    /**
     * Cada letra sin tilde acepta sus variantes con tilde, en minúscula y en mayúscula: así la coincidencia no
     * depende de cómo cada motor trate las mayúsculas fuera del ASCII.
     */
    private static final Map<Character, String> VARIANTES = Map.of(
            'a', "aáàäâAÁÀÄÂ",
            'e', "eéèëêEÉÈËÊ",
            'i', "iíìïîIÍÌÏÎ",
            'o', "oóòöôOÓÒÖÔ",
            'u', "uúùüûUÚÙÜÛ",
            'n', "nñNÑ",
            'c', "cçCÇ");

    public TextoBusqueda {
        palabras = List.copyOf(palabras);
        if (palabras.isEmpty()) {
            throw new IllegalArgumentException("Un texto de búsqueda tiene al menos una palabra");
        }
    }

    /**
     * El texto que escribió el turista, o null si no escribió nada.
     *
     * @throws DatoInvalidoException si, sin los espacios de los extremos, tiene menos de 2 o más de 100 caracteres
     */
    public static TextoBusqueda de(String texto) {
        if (texto == null || texto.isBlank()) {
            return null;
        }
        String recortado = texto.strip();
        if (recortado.length() < LONGITUD_MINIMA || recortado.length() > LONGITUD_MAXIMA) {
            throw new DatoInvalidoException("texto",
                    "debe tener entre %d y %d caracteres".formatted(LONGITUD_MINIMA, LONGITUD_MAXIMA));
        }
        return new TextoBusqueda(Arrays.asList(ESPACIOS.split(normalizar(recortado))));
    }

    /** Sin tildes ni otras marcas, y en minúsculas: "Café" queda "cafe" y "Niño" queda "nino". */
    static String normalizar(String texto) {
        return MARCAS.matcher(Normalizer.normalize(texto, Normalizer.Form.NFD)).replaceAll("")
                .toLowerCase(Locale.ROOT);
    }

    /**
     * Una expresión regular por palabra, sin anclas: la palabra puede estar en cualquier parte del texto. Se evalúan
     * sin distinguir mayúsculas.
     */
    public List<String> expresiones() {
        return palabras.stream().map(TextoBusqueda::expresion).toList();
    }

    /** Si cada palabra aparece en el nombre o en la descripción. */
    public boolean coincideCon(String nombre, String descripcion) {
        String texto = nombre + "\n" + descripcion;
        return expresiones().stream()
                .allMatch(expresion -> Pattern.compile(expresion, Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE)
                        .matcher(texto).find());
    }

    private static String expresion(String palabra) {
        StringBuilder expresion = new StringBuilder();
        for (char letra : palabra.toCharArray()) {
            String variantes = VARIANTES.get(letra);
            if (variantes != null) {
                expresion.append('[').append(variantes).append(']');
            } else if (ESPECIALES.indexOf(letra) >= 0) {
                expresion.append('\\').append(letra);
            } else {
                expresion.append(letra);
            }
        }
        return expresion.toString();
    }
}
