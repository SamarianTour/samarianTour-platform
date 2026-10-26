package co.edu.unimagdalena.samariantour.nucleo.solicitudes.infraestructura.entrada.rest.dto;

import co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.modelo.EstadoSolicitud;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

/**
 * Una solicitud del turista con su historial y su plazo vigente (RF-33, RF-42): lo que la aplicación consulta al
 * reconectar (ADR-04).
 *
 * @param condiciones el precio base y las condiciones, desde que el operador la atiende
 * @param venceEn     el plazo vigente: el del operador mientras está Pendiente, luego la ventana de confirmación y
 *                    pago; se omite si está cerrada
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record SolicitudTuristaResponse(UUID id, OfertaSolicitadaResponse oferta, LocalDate fechaInicio,
                                       @JsonFormat(pattern = "HH:mm") LocalTime horaSalida,
                                       @JsonFormat(pattern = "HH:mm") LocalTime horaReserva, LocalDate fechaSalida,
                                       Integer numeroUnidades, int personas, String nota,
                                       EstadoSolicitud estado, CondicionesResponse condiciones, Instant venceEn,
                                       Instant creadaEn, Instant actualizadaEn, List<TransicionResponse> historial) {
}
