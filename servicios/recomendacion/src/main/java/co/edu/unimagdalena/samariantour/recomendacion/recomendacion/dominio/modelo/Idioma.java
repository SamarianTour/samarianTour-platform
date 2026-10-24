package co.edu.unimagdalena.samariantour.recomendacion.recomendacion.dominio.modelo;

import co.edu.unimagdalena.samariantour.recomendacion.recomendacion.dominio.excepcion.DatoInvalidoException;

import java.util.Arrays;
import java.util.Locale;

/**
 * Idioma de las explicaciones [SUPUESTO] (DR-02): español, el de la aplicación, o inglés.
 */
public enum Idioma {
    ES("es"), EN("en");

    private final String codigo;

    Idioma(String codigo) {
        this.codigo = codigo;
    }

    public String codigo() {
        return codigo;
    }

    /** Sin idioma, español. */
    public static Idioma deCodigo(String codigo) {
        if (codigo == null || codigo.isBlank()) {
            return ES;
        }
        String buscado = codigo.strip().toLowerCase(Locale.ROOT);
        return Arrays.stream(values()).filter(idioma -> idioma.codigo.equals(buscado)).findFirst()
                .orElseThrow(() -> new DatoInvalidoException("idioma", "debe ser es o en"));
    }
}
