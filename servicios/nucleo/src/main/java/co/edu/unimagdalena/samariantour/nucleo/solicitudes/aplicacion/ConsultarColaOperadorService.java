package co.edu.unimagdalena.samariantour.nucleo.solicitudes.aplicacion;

import co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.excepcion.SolicitudNoEncontradaException;
import co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.modelo.AvisoPlataforma;
import co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.modelo.DetalleSolicitudOperador;
import co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.modelo.EstadoSolicitud;
import co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.modelo.Pagina;
import co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.modelo.Paginacion;
import co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.modelo.SolicitudDisponibilidad;
import co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.puerto.entrada.ConsultarColaOperadorUseCase;
import co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.puerto.salida.AvisoPlataformaPort;
import co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.puerto.salida.PrestadoresPort;
import co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.puerto.salida.SolicitudRepositoryPort;

import java.util.UUID;

/**
 * La cola del operador (RF-35, RF-36), el detalle para verificar con el prestador y sus avisos (RF-34). La solicitud
 * asignada a otro operador responde como si no existiera.
 */
public class ConsultarColaOperadorService implements ConsultarColaOperadorUseCase {

    private final SolicitudRepositoryPort solicitudes;
    private final PrestadoresPort prestadores;
    private final AvisoPlataformaPort avisos;

    public ConsultarColaOperadorService(SolicitudRepositoryPort solicitudes, PrestadoresPort prestadores,
                                        AvisoPlataformaPort avisos) {
        this.solicitudes = solicitudes;
        this.prestadores = prestadores;
        this.avisos = avisos;
    }

    @Override
    public Pagina<SolicitudDisponibilidad> cola(UUID operadorId, EstadoSolicitud estado, Paginacion paginacion) {
        return solicitudes.delOperador(operadorId, estado, paginacion);
    }

    @Override
    public DetalleSolicitudOperador detalle(UUID operadorId, UUID solicitudId) {
        SolicitudDisponibilidad solicitud = solicitudes.buscar(solicitudId)
                .filter(encontrada -> encontrada.esDelOperador(operadorId))
                .orElseThrow(() -> new SolicitudNoEncontradaException(solicitudId));
        return new DetalleSolicitudOperador(solicitud,
                prestadores.whatsapp(solicitud.oferta().prestadorId()).orElse(null));
    }

    @Override
    public Pagina<AvisoPlataforma> avisos(UUID operadorId, Paginacion paginacion) {
        return avisos.avisosDe(operadorId, paginacion);
    }
}
