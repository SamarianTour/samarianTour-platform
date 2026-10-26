package co.edu.unimagdalena.samariantour.nucleo.solicitudes.aplicacion;

import co.edu.unimagdalena.samariantour.nucleo.compartido.concurrencia.EjecutorConReintento;
import co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.excepcion.SolicitudNoEncontradaException;
import co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.modelo.CondicionesOfrecidas;
import co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.modelo.SolicitudDisponibilidad;
import co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.puerto.entrada.AtenderSolicitudUseCase;
import co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.puerto.entrada.comando.AtenderSolicitudCommand;
import co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.puerto.salida.NotificacionRepositoryPort;
import co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.puerto.salida.SolicitudRepositoryPort;
import co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.puerto.salida.TransaccionPort;

import java.time.Instant;
import java.time.Clock;

/**
 * El operador atiende una solicitud de su cola (RF-37 a RF-40). Si el vencimiento corre justo a la vez, el bloqueo
 * optimista deja ganar a uno (ADR-05): al reintentar se recarga la solicitud y, si ya venció, el operador recibe 409
 * {@code TRANSICION_INVALIDA} con su estado real.
 */
public class AtenderSolicitudService implements AtenderSolicitudUseCase {

    private final SolicitudRepositoryPort solicitudes;
    private final NotificacionRepositoryPort notificaciones;
    private final TransaccionPort transaccion;
    private final EjecutorConReintento reintento;
    private final Clock reloj;

    public AtenderSolicitudService(SolicitudRepositoryPort solicitudes, NotificacionRepositoryPort notificaciones,
                                   TransaccionPort transaccion, EjecutorConReintento reintento, Clock reloj) {
        this.solicitudes = solicitudes;
        this.notificaciones = notificaciones;
        this.transaccion = transaccion;
        this.reintento = reintento;
        this.reloj = reloj;
    }

    @Override
    public SolicitudDisponibilidad atender(AtenderSolicitudCommand comando) {
        return reintento.ejecutar(() -> transaccion.ejecutar(() -> {
            SolicitudDisponibilidad solicitud = solicitudes.buscar(comando.solicitudId())
                    .filter(encontrada -> encontrada.esDelOperador(comando.operadorId()))
                    .orElseThrow(() -> new SolicitudNoEncontradaException(comando.solicitudId()));
            Instant ahora = reloj.instant();
            switch (comando.resultado()) {
                case ATENDIDA -> solicitud.atender(comando.operadorId(), new CondicionesOfrecidas(comando.precioBase(),
                        comando.politicaCancelacion(), comando.nota()), ahora);
                case NO_DISPONIBLE -> solicitud.marcarNoDisponible(comando.operadorId(), comando.nota(), ahora);
            }
            solicitudes.actualizar(solicitud);
            notificaciones.registrar(solicitud.extraerNotificaciones());
            return solicitud;
        }));
    }
}
