package co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.puerto.salida;

import co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.modelo.NotificacionPendiente;
import co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.modelo.NotificacionSolicitud;

import java.time.Instant;
import java.util.List;

/**
 * Las notificaciones de las solicitudes en PostgreSQL (DN-13). Se registran en la transacción del cambio que las
 * produce: si el cambio se deshace, la notificación tampoco queda.
 */
public interface NotificacionRepositoryPort {

    /**
     * Las de WhatsApp quedan pendientes de envío; las de la plataforma quedan entregadas al confirmar la transacción:
     * el operador las ve en sus avisos.
     */
    void registrar(List<NotificacionSolicitud> notificaciones);

    /**
     * Toma para esta transacción hasta {@code limite} notificaciones de WhatsApp pendientes cuyo intento ya toca. Las
     * que otra transacción tiene tomadas se saltan ({@code FOR UPDATE SKIP LOCKED}), como en la bandeja (DN-06).
     */
    List<NotificacionPendiente> tomarPendientes(Instant ahora, int limite);

    /** Guarda el resultado del envío: enviada, reprogramada o fallida. */
    void guardar(NotificacionPendiente notificacion);
}
