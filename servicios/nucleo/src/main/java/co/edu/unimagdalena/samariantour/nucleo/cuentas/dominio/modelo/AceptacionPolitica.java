package co.edu.unimagdalena.samariantour.nucleo.cuentas.dominio.modelo;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Objects;

/**
 * Constancia de que el usuario aceptó una versión de la política de datos: la versión y el momento (RF-10, RNF-09).
 * Nunca se borra, ni siquiera al suprimir la cuenta.
 */
public record AceptacionPolitica(String version, Instant fechaAceptacion) {

    public AceptacionPolitica {
        Objects.requireNonNull(version, "version");
        fechaAceptacion = Objects.requireNonNull(fechaAceptacion, "fechaAceptacion").truncatedTo(ChronoUnit.MICROS);
    }
}
