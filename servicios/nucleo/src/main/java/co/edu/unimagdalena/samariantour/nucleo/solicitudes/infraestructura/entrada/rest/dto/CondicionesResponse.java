package co.edu.unimagdalena.samariantour.nucleo.solicitudes.infraestructura.entrada.rest.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;

/**
 * El precio base y las condiciones que transmitió el operador (RF-42).
 *
 * @param aparteHasta hasta cuándo el prestador aparta el cupo: el fin de la ventana de confirmación y pago (RF-38)
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record CondicionesResponse(long precioBase, String politicaCancelacion, String nota, Instant aparteHasta) {
}
