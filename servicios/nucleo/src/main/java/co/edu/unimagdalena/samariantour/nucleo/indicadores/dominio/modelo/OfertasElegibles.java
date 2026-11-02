package co.edu.unimagdalena.samariantour.nucleo.indicadores.dominio.modelo;

import java.util.UUID;

/** Las ofertas visibles de un prestador, como las cuenta el catálogo el día de la revisión (DC-51). */
public record OfertasElegibles(UUID prestadorId, long ofertas) {
}
