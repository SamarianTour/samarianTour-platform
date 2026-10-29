package co.edu.unimagdalena.samariantour.nucleo.administracion.infraestructura.entrada.rest.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * La comisión vigente y su historial (RF-82, RF-83).
 *
 * @param historial del cambio más reciente al más antiguo; el primero es el vigente
 */
public record ComisionResponse(BigDecimal porcentaje, Instant vigenteDesde, List<CambioComisionResponse> historial) {
}
