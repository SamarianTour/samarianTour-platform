package co.edu.unimagdalena.samariantour.recomendacion.recomendacion.infraestructura.entrada.rest.dto;

import java.time.Instant;
import java.util.List;

/**
 * Esquema {@code AlertasZona}: las alertas activas de una zona y la hora de la lectura de la que salen.
 *
 * @param alertas vacía si la zona no tiene alertas activas
 */
public record AlertasZonaResponse(String zonaId, Instant actualizadoEn, List<AlertaResponse> alertas) {
}
