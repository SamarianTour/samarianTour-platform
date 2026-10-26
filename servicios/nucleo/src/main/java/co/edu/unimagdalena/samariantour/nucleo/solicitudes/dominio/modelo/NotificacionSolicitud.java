package co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.modelo;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Una notificación que produjo un cambio de la solicitud (DN-13). Se guarda en la misma transacción que el cambio y
 * se envía después: nunca se notifica algo que no quedó guardado.
 *
 * @param datos los parámetros de la plantilla: nombre de la oferta, fecha, precio, plazo... Nunca datos de contacto.
 */
public record NotificacionSolicitud(UUID id, UUID solicitudId, Destinatario destinatario, CanalNotificacion canal,
                                    TipoNotificacion tipo, Map<String, String> datos, Instant creadaEn) {

    public NotificacionSolicitud {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(solicitudId, "solicitudId");
        Objects.requireNonNull(destinatario, "destinatario");
        Objects.requireNonNull(canal, "canal");
        Objects.requireNonNull(tipo, "tipo");
        datos = Map.copyOf(datos);
        creadaEn = Objects.requireNonNull(creadaEn, "creadaEn").truncatedTo(ChronoUnit.MICROS);
    }
}
