package co.edu.unimagdalena.samariantour.recomendacion.recomendacion.dominio.modelo;

import java.util.List;
import java.util.Objects;

/**
 * Por qué se recomienda una oferta (RF-27): los criterios explícitos que cumple y un texto para el turista.
 */
public record Explicacion(List<CriterioAfinidad> criterios, String texto) {

    public static final int TEXTO_MAXIMO = 300;

    public Explicacion {
        criterios = List.copyOf(criterios);
        Objects.requireNonNull(texto, "texto");
        if (texto.isBlank() || texto.length() > TEXTO_MAXIMO) {
            throw new IllegalArgumentException("La explicación debe tener entre 1 y %d caracteres".formatted(TEXTO_MAXIMO));
        }
    }
}
