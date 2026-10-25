package co.edu.unimagdalena.samariantour.recomendacion.recomendacion.dominio.modelo;

import java.util.Objects;

/**
 * Un texto para el turista en los dos idiomas de la aplicación (DR-02, DR-21). Los dos son obligatorios: una alerta no
 * se muestra a medias traducida.
 */
public record TextoTraducido(String es, String en) {

    public TextoTraducido {
        if (Objects.requireNonNull(es, "es").isBlank() || Objects.requireNonNull(en, "en").isBlank()) {
            throw new IllegalArgumentException("El texto debe tener contenido en español y en inglés");
        }
    }

    /** El texto en ese idioma. */
    public String para(Idioma idioma) {
        return idioma == Idioma.EN ? en : es;
    }
}
