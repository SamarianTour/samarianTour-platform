package co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.puerto.entrada;

import co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.modelo.PeticionIdempotente;
import co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.modelo.RespuestaGuardada;
import co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.modelo.ResultadoIdempotente;
import co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.modelo.SolicitudDisponibilidad;
import co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.puerto.entrada.comando.ResponderCondicionesCommand;

import java.util.function.Function;

/**
 * El turista confirma o declina las condiciones de una solicitud atendida (RF-42, RF-43), con su clave de idempotencia
 * (ADR-04): repetir la confirmación con la misma clave devuelve la misma respuesta.
 */
public interface ResponderCondicionesUseCase {

    /**
     * @param respuesta cómo se responde la solicitud ya cambiada; se guarda con la clave
     * @throws co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.excepcion.SolicitudNoEncontradaException si
     *         no existe o es de otro turista
     * @throws co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.excepcion.TransicionInvalidaException si no
     *         está Atendida
     * @throws co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.excepcion.PlazoVencidoException si la ventana
     *         ya terminó
     */
    ResultadoIdempotente responder(ResponderCondicionesCommand comando, PeticionIdempotente peticion,
                                   Function<SolicitudDisponibilidad, RespuestaGuardada> respuesta);
}
