package co.edu.unimagdalena.samariantour.nucleo.pagos.dominio.puerto.entrada;

import co.edu.unimagdalena.samariantour.nucleo.pagos.dominio.modelo.IntentoPago;
import co.edu.unimagdalena.samariantour.nucleo.pagos.dominio.modelo.PeticionIdempotente;
import co.edu.unimagdalena.samariantour.nucleo.pagos.dominio.modelo.RespuestaGuardada;
import co.edu.unimagdalena.samariantour.nucleo.pagos.dominio.modelo.ResultadoIdempotente;
import co.edu.unimagdalena.samariantour.nucleo.pagos.dominio.modelo.SesionGenerada;

import java.util.List;
import java.util.UUID;
import java.util.function.Function;

/** El turista abre el pago de su solicitud confirmada (RF-46 a RF-49, RF-55, RF-58, DN-19, DN-20). */
public interface GenerarSesionCobroUseCase {

    /**
     * Si ya hay un intento en curso devuelve ese mismo; si no, abre el siguiente, que aplaza el vencimiento mientras
     * dure (RF-55). Con la misma clave de idempotencia, la misma respuesta.
     *
     * @param respuesta arma la respuesta que se guarda con la clave
     * @throws co.edu.unimagdalena.samariantour.nucleo.pagos.dominio.excepcion.SolicitudNoEncontradaException
     * @throws co.edu.unimagdalena.samariantour.nucleo.pagos.dominio.excepcion.SolicitudNoCobrableException
     * @throws co.edu.unimagdalena.samariantour.nucleo.pagos.dominio.excepcion.IntentosAgotadosException
     */
    ResultadoIdempotente generar(UUID solicitudId, UUID turistaId, PeticionIdempotente peticion,
                                 Function<SesionGenerada, RespuestaGuardada> respuesta);

    /** Los intentos de una solicitud del turista, para reconsultar al reconectar (ADR-04). */
    List<IntentoPago> intentos(UUID solicitudId, UUID turistaId);
}
