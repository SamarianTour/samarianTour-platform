package co.edu.unimagdalena.samariantour.recomendacion.recomendacion.dominio.modelo;

import co.edu.unimagdalena.samariantour.recomendacion.recomendacion.dominio.excepcion.DatoInvalidoException;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

/**
 * Lo que busca el turista [SUPUESTO] (DR-02). Nada lo identifica: ni nombre, ni id, ni ubicación.
 *
 * @param texto                lo que busca, en sus palabras; opcional, máximo 500 caracteres
 * @param idioma               de las explicaciones; por defecto español
 * @param zonaId               una zona del catálogo; opcional
 * @param accesibleSillaRuedas filtro duro: si es true, solo ofertas accesibles
 * @param nivelEsfuerzoMaximo  filtro duro; opcional
 * @param precioMaximo         filtro duro, en pesos; opcional y mayor que cero
 */
public record Preferencias(String texto, Idioma idioma, Set<TipoOferta> tiposOferta, Set<TipoActividad> tiposActividad,
                           String zonaId, boolean accesibleSillaRuedas, NivelEsfuerzo nivelEsfuerzoMaximo,
                           BigDecimal precioMaximo) {

    public static final int TEXTO_MAXIMO = 500;

    /** Exige texto o al menos una preferencia; deja los textos sin espacios en los extremos y vacíos como null. */
    public Preferencias {
        texto = sinBlancos(texto);
        if (texto != null && texto.length() > TEXTO_MAXIMO) {
            throw new DatoInvalidoException("texto", "máximo %d caracteres".formatted(TEXTO_MAXIMO));
        }
        idioma = idioma == null ? Idioma.ES : idioma;
        tiposOferta = copia(tiposOferta, TipoOferta.class);
        tiposActividad = copia(tiposActividad, TipoActividad.class);
        zonaId = sinBlancos(zonaId);
        if (precioMaximo != null && precioMaximo.signum() <= 0) {
            throw new DatoInvalidoException("precioMaximo", "debe ser mayor que cero");
        }
        if (texto == null && tiposOferta.isEmpty() && tiposActividad.isEmpty() && zonaId == null
                && !accesibleSillaRuedas && nivelEsfuerzoMaximo == null && precioMaximo == null) {
            throw new DatoInvalidoException("preferencias", "escribe qué buscas o elige al menos un filtro");
        }
    }

    public boolean tieneTexto() {
        return texto != null;
    }

    /** Sin el texto: el texto libre del turista nunca va a un log (DR-09). */
    @Override
    public String toString() {
        return "Preferencias[texto=%s, idioma=%s, tiposOferta=%s, tiposActividad=%s, zonaId=%s, accesible=%s, "
                .formatted(tieneTexto() ? "(omitido)" : "-", idioma, tiposOferta, tiposActividad, zonaId,
                        accesibleSillaRuedas)
                + "nivelEsfuerzoMaximo=%s, precioMaximo=%s]".formatted(nivelEsfuerzoMaximo, precioMaximo);
    }

    private static String sinBlancos(String valor) {
        return valor == null || valor.isBlank() ? null : valor.strip();
    }

    /** En el orden de declaración del enum, para que las explicaciones salgan siempre igual. */
    static <E extends Enum<E>> Set<E> copia(Set<E> valores, Class<E> tipo) {
        return Collections.unmodifiableSet(valores == null || valores.isEmpty()
                ? EnumSet.noneOf(tipo)
                : EnumSet.copyOf(valores));
    }
}
