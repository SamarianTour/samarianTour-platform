package co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.puerto.entrada;

import co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.modelo.AvisoPlataforma;
import co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.modelo.DetalleSolicitudOperador;
import co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.modelo.EstadoSolicitud;
import co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.modelo.Pagina;
import co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.modelo.Paginacion;
import co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.modelo.SolicitudDisponibilidad;

import java.util.UUID;

/** La cola del operador, su historial de trámites y sus avisos (RF-34 a RF-36). */
public interface ConsultarColaOperadorUseCase {

    /**
     * Sus solicitudes en ese estado: las pendientes de la más antigua a la más reciente (RF-35); las demás, de la más
     * reciente a la más antigua.
     */
    Pagina<SolicitudDisponibilidad> cola(UUID operadorId, EstadoSolicitud estado, Paginacion paginacion);

    /**
     * @throws co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.excepcion.SolicitudNoEncontradaException si
     *         no existe o está asignada a otro operador
     */
    DetalleSolicitudOperador detalle(UUID operadorId, UUID solicitudId);

    /** Sus avisos en la plataforma, del más reciente al más antiguo (RF-34). */
    Pagina<AvisoPlataforma> avisos(UUID operadorId, Paginacion paginacion);
}
