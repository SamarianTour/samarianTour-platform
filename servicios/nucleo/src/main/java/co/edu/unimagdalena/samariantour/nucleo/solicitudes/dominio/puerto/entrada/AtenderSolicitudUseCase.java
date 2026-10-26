package co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.puerto.entrada;

import co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.modelo.SolicitudDisponibilidad;
import co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.puerto.entrada.comando.AtenderSolicitudCommand;

/**
 * El operador marca una solicitud de su cola como Atendida, con el precio base y las condiciones, o como No disponible
 * (RF-37 a RF-40). Solo el operador asignado.
 */
public interface AtenderSolicitudUseCase {

    /**
     * @throws co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.excepcion.SolicitudNoEncontradaException si
     *         no existe o está asignada a otro operador
     * @throws co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.excepcion.TransicionInvalidaException si ya
     *         no está Pendiente, por ejemplo porque venció mientras tanto
     */
    SolicitudDisponibilidad atender(AtenderSolicitudCommand comando);
}
