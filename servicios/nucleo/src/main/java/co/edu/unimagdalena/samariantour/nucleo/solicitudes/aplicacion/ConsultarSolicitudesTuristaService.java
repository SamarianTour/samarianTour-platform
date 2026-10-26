package co.edu.unimagdalena.samariantour.nucleo.solicitudes.aplicacion;

import co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.excepcion.SolicitudNoEncontradaException;
import co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.modelo.Pagina;
import co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.modelo.Paginacion;
import co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.modelo.SolicitudDisponibilidad;
import co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.puerto.entrada.ConsultarSolicitudesTuristaUseCase;
import co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.puerto.salida.SolicitudRepositoryPort;

import java.util.UUID;

/** Las solicitudes del turista (RF-33). La de otro turista responde como si no existiera. */
public class ConsultarSolicitudesTuristaService implements ConsultarSolicitudesTuristaUseCase {

    private final SolicitudRepositoryPort solicitudes;

    public ConsultarSolicitudesTuristaService(SolicitudRepositoryPort solicitudes) {
        this.solicitudes = solicitudes;
    }

    @Override
    public Pagina<SolicitudDisponibilidad> listar(UUID turistaId, Paginacion paginacion) {
        return solicitudes.delTurista(turistaId, paginacion);
    }

    @Override
    public SolicitudDisponibilidad consultar(UUID turistaId, UUID solicitudId) {
        return solicitudes.buscar(solicitudId)
                .filter(solicitud -> solicitud.esDelTurista(turistaId))
                .orElseThrow(() -> new SolicitudNoEncontradaException(solicitudId));
    }
}
