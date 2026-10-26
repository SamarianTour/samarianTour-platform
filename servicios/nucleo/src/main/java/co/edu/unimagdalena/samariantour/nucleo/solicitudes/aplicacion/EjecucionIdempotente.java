package co.edu.unimagdalena.samariantour.nucleo.solicitudes.aplicacion;

import co.edu.unimagdalena.samariantour.nucleo.compartido.concurrencia.EjecutorConReintento;
import co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.excepcion.ClaveIdempotenciaReutilizadaException;
import co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.modelo.OperacionRegistrada;
import co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.modelo.PeticionIdempotente;
import co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.modelo.RespuestaGuardada;
import co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.modelo.ResultadoIdempotente;
import co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.puerto.salida.IdempotenciaPort;
import co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.puerto.salida.TransaccionPort;

import java.time.Clock;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * Ejecuta una operación del turista con su clave de idempotencia (ADR-04, DN-12), en una sola transacción:
 * <ol>
 *   <li>reserva la clave; si ya se usó con la misma petición, devuelve la respuesta original sin hacer nada, y si se
 *   usó con otra, responde 422 {@code CLAVE_IDEMPOTENCIA_REUTILIZADA};</li>
 *   <li>ejecuta la operación y guarda su respuesta junto con lo que la operación cambió.</li>
 * </ol>
 * Si la operación falla (por ejemplo, el catálogo no responde), la transacción se deshace con la reserva: el turista
 * puede reintentar con la misma clave, como pide el ADR-04. Solo se guardan las respuestas exitosas. Ante un conflicto
 * de concurrencia se reintenta todo, cada vez en una transacción nueva (ADR-05).
 */
public class EjecucionIdempotente {

    private final IdempotenciaPort claves;
    private final TransaccionPort transaccion;
    private final EjecutorConReintento reintento;
    private final Clock reloj;

    public EjecucionIdempotente(IdempotenciaPort claves, TransaccionPort transaccion, EjecutorConReintento reintento,
                                Clock reloj) {
        this.claves = claves;
        this.transaccion = transaccion;
        this.reintento = reintento;
        this.reloj = reloj;
    }

    /**
     * @param operacion lo que hace la operación y su respuesta ya convertida; puede ejecutarse más de una vez si hay
     *                  un conflicto de concurrencia, así que no debe tener efectos fuera de la base
     */
    public ResultadoIdempotente ejecutar(PeticionIdempotente peticion, Supplier<RespuestaGuardada> operacion) {
        return reintento.ejecutar(() -> transaccion.ejecutar(() -> {
            Optional<OperacionRegistrada> previa = claves.reservar(peticion, reloj.instant());
            if (previa.isPresent()) {
                if (!previa.get().coincideCon(peticion)) {
                    throw new ClaveIdempotenciaReutilizadaException(peticion.clave());
                }
                return new ResultadoIdempotente(previa.get().respuesta(), true);
            }
            RespuestaGuardada respuesta = operacion.get();
            claves.completar(peticion, respuesta);
            return new ResultadoIdempotente(respuesta, false);
        }));
    }
}
