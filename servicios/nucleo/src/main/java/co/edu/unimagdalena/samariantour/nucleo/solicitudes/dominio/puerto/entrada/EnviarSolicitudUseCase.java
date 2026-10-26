package co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.puerto.entrada;

import co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.modelo.PeticionIdempotente;
import co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.modelo.RespuestaGuardada;
import co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.modelo.ResultadoIdempotente;
import co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.modelo.SolicitudDisponibilidad;
import co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.puerto.entrada.comando.EnviarSolicitudCommand;

import java.util.function.Function;

/**
 * El turista envía una solicitud de disponibilidad sobre una oferta visible (RF-30 a RF-32, RF-34), con su clave de
 * idempotencia (ADR-04): repetir el envío con la misma clave no crea otra.
 */
public interface EnviarSolicitudUseCase {

    /**
     * @param respuesta cómo se responde la solicitud creada; es lo que se guarda con la clave y se repite si el turista
     *                  reintenta
     * @throws co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.excepcion.ExcepcionDominio si no se puede
     *         crear: oferta no visible, catálogo caído, WhatsApp sin verificar, prestador sin operador, duplicada o un
     *         dato inválido. No queda nada guardado.
     */
    ResultadoIdempotente enviar(EnviarSolicitudCommand comando, PeticionIdempotente peticion,
                                Function<SolicitudDisponibilidad, RespuestaGuardada> respuesta);
}
