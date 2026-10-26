package co.edu.unimagdalena.samariantour.nucleo.solicitudes.infraestructura.entrada.rest.dto;

import co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.modelo.EstadoSolicitud;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

/**
 * Una solicitud en la cola del operador (RF-35, RF-36).
 *
 * @param proximaAVencer pendiente con 6 h o menos para atenderla: se resalta
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record SolicitudColaResponse(UUID id, OfertaSolicitadaResponse oferta, LocalDate fechaInicio,
                                    @JsonFormat(pattern = "HH:mm") LocalTime horaSalida,
                                    @JsonFormat(pattern = "HH:mm") LocalTime horaReserva, LocalDate fechaSalida,
                                    Integer numeroUnidades, int personas,
                                    EstadoSolicitud estado, Instant venceEn, boolean proximaAVencer, Instant creadaEn) {
}
