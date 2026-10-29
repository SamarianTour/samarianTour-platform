package co.edu.unimagdalena.samariantour.nucleo.cuentas.dominio.modelo;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Objects;
import java.util.UUID;

/**
 * Constancia de que el titular pidió eliminar sus datos personales (RF-13, Ley 1581) y de cuándo se cumplió.
 *
 * @param fechaEjecucion vacío mientras está {@link EstadoSupresion#SOLICITADA}
 */
public record SolicitudSupresion(UUID id, Instant fechaSolicitud, EstadoSupresion estado, Instant fechaEjecucion) {

    public SolicitudSupresion {
        Objects.requireNonNull(id, "id");
        fechaSolicitud = Objects.requireNonNull(fechaSolicitud, "fechaSolicitud").truncatedTo(ChronoUnit.MICROS);
        Objects.requireNonNull(estado, "estado");
        fechaEjecucion = fechaEjecucion == null ? null : fechaEjecucion.truncatedTo(ChronoUnit.MICROS);
        if ((estado == EstadoSupresion.EJECUTADA) != (fechaEjecucion != null)) {
            throw new IllegalArgumentException("Solo una supresión ejecutada tiene fecha de ejecución");
        }
    }
}
