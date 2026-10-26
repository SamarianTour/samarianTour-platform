package co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.modelo;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Un aviso al operador dentro de la plataforma (RF-34): el mismo registro que la notificación, con el canal
 * PLATAFORMA [SUPUESTO] (DN-13).
 */
public record AvisoPlataforma(UUID id, UUID solicitudId, TipoNotificacion tipo, Map<String, String> datos,
                              Instant creadoEn) {

    public AvisoPlataforma {
        datos = Map.copyOf(datos);
    }
}
