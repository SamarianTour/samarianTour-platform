package co.edu.unimagdalena.samariantour.nucleo.solicitudes.infraestructura.entrada.rest.dto;

import co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.modelo.ActorTransicion;
import co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.modelo.EstadoSolicitud;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;

/**
 * Un cambio del historial (RF-33). No lleva quién lo hizo: el identificador del operador no es del turista.
 *
 * @param de vacío en la creación
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record TransicionResponse(EstadoSolicitud de, EstadoSolicitud a, Instant en, ActorTransicion actor,
                                 String motivo) {
}
