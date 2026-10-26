package co.edu.unimagdalena.samariantour.nucleo.solicitudes.infraestructura.entrada.rest.dto;

import co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.modelo.TipoNotificacion;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/** Un aviso del operador en la plataforma (RF-34), con los datos de la solicitud que lo produjo. */
public record AvisoResponse(UUID id, TipoNotificacion tipo, UUID solicitudId, Map<String, String> datos,
                            Instant creadoEn) {
}
