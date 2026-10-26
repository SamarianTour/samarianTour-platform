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
 * Una solicitud como la ve el operador para verificarla con el prestador (RF-37, DN-16).
 *
 * @param whatsappPrestador para contactarlo fuera de la plataforma (RF-71); se omite si no tiene uno registrado
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record SolicitudOperadorResponse(UUID id, OfertaSolicitadaResponse oferta, UUID prestadorId,
                                        String whatsappPrestador, LocalDate fechaInicio,
                                        @JsonFormat(pattern = "HH:mm") LocalTime horaSalida,
                                        @JsonFormat(pattern = "HH:mm") LocalTime horaReserva,
                                        LocalDate fechaSalida, Integer numeroUnidades, int personas, String nota,
                                        EstadoSolicitud estado, CondicionesResponse condiciones, Instant venceEn,
                                        boolean proximaAVencer, Instant creadaEn, Instant actualizadaEn,
                                        List<TransicionResponse> historial) {
}
