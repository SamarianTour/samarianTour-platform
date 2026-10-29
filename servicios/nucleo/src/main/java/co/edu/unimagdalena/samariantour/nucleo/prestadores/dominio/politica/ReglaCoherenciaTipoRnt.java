package co.edu.unimagdalena.samariantour.nucleo.prestadores.dominio.politica;

import co.edu.unimagdalena.samariantour.nucleo.prestadores.dominio.excepcion.TipoNoCoincideConRntException;
import co.edu.unimagdalena.samariantour.nucleo.prestadores.dominio.modelo.TipoPrestador;

import java.text.Normalizer;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * El tipo que declara el prestador tiene que corresponder a la categoría de su RNT (RF-68, RF-70, DN-33). Las
 * categorías son las del conjunto del RNT en datos.gov.co; se comparan sin tildes ni mayúsculas. Las que admite cada
 * tipo son [SUPUESTO] hasta validarlas con el Ministerio:
 * <ul>
 *   <li>alojamiento: establecimientos de alojamiento turístico, viviendas turísticas y otros hospedajes;</li>
 *   <li>restaurante: establecimientos de gastronomía;</li>
 *   <li>agencia operadora: agencias de viajes;</li>
 *   <li>guía independiente: guías de turismo, y además persona natural (RF-70).</li>
 * </ul>
 */
public final class ReglaCoherenciaTipoRnt {

    private static final Map<TipoPrestador, List<String>> CATEGORIAS = Map.of(
            TipoPrestador.ALOJAMIENTO, List.of("ESTABLECIMIENTOS DE ALOJAMIENTO TURISTICO", "VIVIENDAS TURISTICAS",
                    "OTROS TIPOS DE HOSPEDAJE TURISTICOS NO PERMANENTES"),
            TipoPrestador.RESTAURANTE, List.of("ESTABLECIMIENTOS DE GASTRONOMIA",
                    "ESTABLECIMIENTOS DE GASTRONOMIA Y SIMILARES"),
            TipoPrestador.AGENCIA_OPERADORA, List.of("AGENCIAS DE VIAJES"),
            TipoPrestador.GUIA_INDEPENDIENTE, List.of("GUIAS DE TURISMO"));

    private ReglaCoherenciaTipoRnt() {
    }

    public static boolean coincide(TipoPrestador tipo, String categoriaRnt, boolean personaNatural) {
        boolean categoria = CATEGORIAS.get(tipo).contains(normalizar(categoriaRnt));
        return tipo == TipoPrestador.GUIA_INDEPENDIENTE ? categoria && personaNatural : categoria;
    }

    /** @throws TipoNoCoincideConRntException si no coinciden */
    public static void verificar(TipoPrestador tipo, String categoriaRnt, boolean personaNatural) {
        if (!coincide(tipo, categoriaRnt, personaNatural)) {
            throw new TipoNoCoincideConRntException(tipo, categoriaRnt, personaNatural);
        }
    }

    static String normalizar(String texto) {
        return Normalizer.normalize(texto == null ? "" : texto, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .strip().replaceAll("\\s+", " ").toUpperCase(Locale.ROOT);
    }
}
