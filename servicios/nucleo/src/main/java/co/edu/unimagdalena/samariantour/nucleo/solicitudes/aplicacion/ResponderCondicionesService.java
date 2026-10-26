package co.edu.unimagdalena.samariantour.nucleo.solicitudes.aplicacion;

import co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.excepcion.SolicitudNoEncontradaException;
import co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.modelo.PeticionIdempotente;
import co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.modelo.RespuestaGuardada;
import co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.modelo.ResultadoIdempotente;
import co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.modelo.SolicitudDisponibilidad;
import co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.puerto.entrada.ResponderCondicionesUseCase;
import co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.puerto.entrada.comando.ResponderCondicionesCommand;
import co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.puerto.salida.NotificacionRepositoryPort;
import co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.puerto.salida.SolicitudRepositoryPort;

import java.time.Clock;
import java.util.function.Function;

/**
 * El turista confirma o declina (RF-43), dentro de la transacción de su clave de idempotencia (DN-12): repetir la
 * misma respuesta con la misma clave no hace nada más. Declinar libera el cupo y avisa al prestador (RF-56). Si el
 * vencimiento la cambió a la vez, se reintenta y el turista ve el estado real (ADR-05).
 */
public class ResponderCondicionesService implements ResponderCondicionesUseCase {

    private final SolicitudRepositoryPort solicitudes;
    private final NotificacionRepositoryPort notificaciones;
    private final EjecucionIdempotente idempotente;
    private final Clock reloj;

    public ResponderCondicionesService(SolicitudRepositoryPort solicitudes, NotificacionRepositoryPort notificaciones,
                                       EjecucionIdempotente idempotente, Clock reloj) {
        this.solicitudes = solicitudes;
        this.notificaciones = notificaciones;
        this.idempotente = idempotente;
        this.reloj = reloj;
    }

    @Override
    public ResultadoIdempotente responder(ResponderCondicionesCommand comando, PeticionIdempotente peticion,
                                          Function<SolicitudDisponibilidad, RespuestaGuardada> respuesta) {
        return idempotente.ejecutar(peticion, () -> respuesta.apply(aplicar(comando)));
    }

    private SolicitudDisponibilidad aplicar(ResponderCondicionesCommand comando) {
        SolicitudDisponibilidad solicitud = solicitudes.buscar(comando.solicitudId())
                .filter(encontrada -> encontrada.esDelTurista(comando.turistaId()))
                .orElseThrow(() -> new SolicitudNoEncontradaException(comando.solicitudId()));
        solicitud.responder(comando.turistaId(), comando.decision(), reloj.instant());
        solicitudes.actualizar(solicitud);
        notificaciones.registrar(solicitud.extraerNotificaciones());
        return solicitud;
    }
}
