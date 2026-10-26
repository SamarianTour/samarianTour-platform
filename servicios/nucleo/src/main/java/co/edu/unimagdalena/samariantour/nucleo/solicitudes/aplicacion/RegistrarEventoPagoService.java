package co.edu.unimagdalena.samariantour.nucleo.solicitudes.aplicacion;

import co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.excepcion.SolicitudNoEncontradaException;
import co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.modelo.DatosCobro;
import co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.modelo.EstadoSolicitud;
import co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.modelo.ResultadoEventoPago;
import co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.modelo.SolicitudDisponibilidad;
import co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.puerto.entrada.RegistrarEventoPagoUseCase;
import co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.puerto.salida.NotificacionRepositoryPort;
import co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.puerto.salida.SolicitudRepositoryPort;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;

/**
 * Aplica a la solicitud lo que pasa con su pago (RF-50, RF-55, RF-58, DN-17). Los cambios no abren una transacción:
 * exigen la de pagos ({@code MANDATORY}), para que el pago y la solicitud se confirmen juntos. Tampoco reintentan ante
 * un conflicto de concurrencia: lo hace pagos con su transacción completa (ADR-05).
 */
public class RegistrarEventoPagoService implements RegistrarEventoPagoUseCase {

    private final SolicitudRepositoryPort solicitudes;
    private final NotificacionRepositoryPort notificaciones;
    private final Clock reloj;

    public RegistrarEventoPagoService(SolicitudRepositoryPort solicitudes, NotificacionRepositoryPort notificaciones,
                                      Clock reloj) {
        this.solicitudes = solicitudes;
        this.notificaciones = notificaciones;
        this.reloj = reloj;
    }

    @Override
    public Optional<DatosCobro> consultarParaCobro(UUID solicitudId, UUID turistaId) {
        return solicitudes.buscar(solicitudId)
                .filter(solicitud -> solicitud.esDelTurista(turistaId))
                .filter(solicitud -> solicitud.estado() == EstadoSolicitud.CONFIRMADA
                        || solicitud.estado() == EstadoSolicitud.PAGO_FALLIDO)
                .map(solicitud -> new DatosCobro(solicitud.id(), solicitud.estado(),
                        solicitud.condiciones().precioBase(), solicitud.venceEn(), solicitud.oferta().prestadorId(),
                        solicitud.ofertaId()));
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public ResultadoEventoPago marcarIntentoEnCurso(UUID solicitudId) {
        return aplicar(solicitudId, solicitud -> solicitud.marcarIntentoEnCurso(reloj.instant()));
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public ResultadoEventoPago registrarPagoAprobado(UUID solicitudId) {
        return aplicar(solicitudId, solicitud -> solicitud.registrarPagoAprobado(reloj.instant()));
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public ResultadoEventoPago registrarPagoRechazado(UUID solicitudId) {
        return aplicar(solicitudId, solicitud -> solicitud.registrarPagoRechazado(reloj.instant()));
    }

    /** Guarda solo si el evento cambió algo: un pago tardío puede haber vencido la solicitud de paso. */
    private ResultadoEventoPago aplicar(UUID solicitudId,
                                        Function<SolicitudDisponibilidad, ResultadoEventoPago> evento) {
        SolicitudDisponibilidad solicitud = solicitudes.buscar(solicitudId)
                .orElseThrow(() -> new SolicitudNoEncontradaException(solicitudId));
        ResultadoEventoPago resultado = evento.apply(solicitud);
        if (solicitud.tieneCambios()) {
            solicitudes.actualizar(solicitud);
            notificaciones.registrar(solicitud.extraerNotificaciones());
        }
        return resultado;
    }
}
