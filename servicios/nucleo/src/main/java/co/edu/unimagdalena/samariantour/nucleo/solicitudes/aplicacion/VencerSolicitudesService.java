package co.edu.unimagdalena.samariantour.nucleo.solicitudes.aplicacion;

import co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.modelo.SolicitudDisponibilidad;
import co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.puerto.entrada.VencerSolicitudesUseCase;
import co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.puerto.salida.NotificacionRepositoryPort;
import co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.puerto.salida.SolicitudRepositoryPort;
import co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.puerto.salida.TransaccionPort;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

/**
 * Vence un lote de solicitudes (RF-41, RF-55, RF-56, DN-14). El lote se toma con {@code FOR UPDATE SKIP LOCKED}: las
 * dos réplicas del núcleo vencen en paralelo sin tomar la misma, sin un candado distribuido. Como las filas quedan
 * bloqueadas, el operador o el turista que la cambien a la vez esperan, chocan con la revisión y ven el estado real.
 * Cada vencimiento deja su transición y sus notificaciones en la misma transacción.
 */
public class VencerSolicitudesService implements VencerSolicitudesUseCase {

    /** Pocas por lote, para no retener los bloqueos mucho tiempo. */
    public static final int TAMANO_LOTE = 50;

    private final SolicitudRepositoryPort solicitudes;
    private final NotificacionRepositoryPort notificaciones;
    private final TransaccionPort transaccion;
    private final Clock reloj;
    private final int tamanoLote;

    public VencerSolicitudesService(SolicitudRepositoryPort solicitudes, NotificacionRepositoryPort notificaciones,
                                    TransaccionPort transaccion, Clock reloj) {
        this(solicitudes, notificaciones, transaccion, reloj, TAMANO_LOTE);
    }

    public VencerSolicitudesService(SolicitudRepositoryPort solicitudes, NotificacionRepositoryPort notificaciones,
                                    TransaccionPort transaccion, Clock reloj, int tamanoLote) {
        this.solicitudes = solicitudes;
        this.notificaciones = notificaciones;
        this.transaccion = transaccion;
        this.reloj = reloj;
        this.tamanoLote = tamanoLote;
    }

    @Override
    public int vencerLote() {
        return transaccion.ejecutar(() -> {
            Instant ahora = reloj.instant();
            List<SolicitudDisponibilidad> porVencer = solicitudes.tomarPorVencer(ahora, tamanoLote);
            int vencidas = 0;
            for (SolicitudDisponibilidad solicitud : porVencer) {
                if (solicitud.vencer(ahora)) {
                    solicitudes.actualizar(solicitud);
                    notificaciones.registrar(solicitud.extraerNotificaciones());
                    vencidas++;
                }
            }
            return vencidas;
        });
    }
}
