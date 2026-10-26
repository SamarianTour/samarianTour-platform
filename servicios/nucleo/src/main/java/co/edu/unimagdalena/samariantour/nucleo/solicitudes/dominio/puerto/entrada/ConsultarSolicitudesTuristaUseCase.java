package co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.puerto.entrada;

import co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.modelo.Pagina;
import co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.modelo.Paginacion;
import co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.modelo.SolicitudDisponibilidad;

import java.util.UUID;

/**
 * El turista consulta sus solicitudes y el estado real de cada una (RF-33). Es la reconsulta al reconectar del ADR-04:
 * si una petición quedó sin respuesta, la aplicación pregunta aquí en lugar de suponer.
 */
public interface ConsultarSolicitudesTuristaUseCase {

    /** Las del turista, de la más reciente a la más antigua. */
    Pagina<SolicitudDisponibilidad> listar(UUID turistaId, Paginacion paginacion);

    /**
     * Con su historial y su plazo vigente.
     *
     * @throws co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.excepcion.SolicitudNoEncontradaException si
     *         no existe o es de otro turista
     */
    SolicitudDisponibilidad consultar(UUID turistaId, UUID solicitudId);
}
