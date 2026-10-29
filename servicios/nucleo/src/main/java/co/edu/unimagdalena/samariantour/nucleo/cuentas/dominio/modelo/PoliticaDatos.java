package co.edu.unimagdalena.samariantour.nucleo.cuentas.dominio.modelo;

import java.time.LocalDate;
import java.util.Objects;

/**
 * Una versión publicada de la política de tratamiento de datos (RF-09 a RF-11). La vigente es la de publicación más
 * reciente.
 *
 * @param version  identificador de la versión, por ejemplo "v1"; es lo que queda en cada aceptación (RF-10)
 * @param urlTexto dónde se lee el texto completo de esa versión
 */
public record PoliticaDatos(String version, LocalDate fechaPublicacion, String urlTexto) {

    public PoliticaDatos {
        Objects.requireNonNull(version, "version");
        Objects.requireNonNull(fechaPublicacion, "fechaPublicacion");
        Objects.requireNonNull(urlTexto, "urlTexto");
    }
}
