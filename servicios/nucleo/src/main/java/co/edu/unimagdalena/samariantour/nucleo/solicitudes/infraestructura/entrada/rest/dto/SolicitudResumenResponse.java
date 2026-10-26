package co.edu.unimagdalena.samariantour.nucleo.solicitudes.infraestructura.entrada.rest.dto;

import co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.modelo.EstadoSolicitud;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

/**
 * Una solicitud en la lista del turista (RF-33).
 *
 * @param venceEn el plazo vigente; se omite si está cerrada
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record SolicitudResumenResponse(UUID id, OfertaSolicitadaResponse oferta, LocalDate fechaInicio,
                                       @JsonFormat(pattern = "HH:mm") LocalTime horaSalida,
                                       @JsonFormat(pattern = "HH:mm") LocalTime horaReserva, LocalDate fechaSalida,
                                       Integer numeroUnidades, int personas,
                                       EstadoSolicitud estado, Instant venceEn, Instant creadaEn,
                                       Instant actualizadaEn) {
}
