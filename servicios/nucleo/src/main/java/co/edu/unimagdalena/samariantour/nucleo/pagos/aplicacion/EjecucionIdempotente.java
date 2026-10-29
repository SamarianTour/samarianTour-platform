package co.edu.unimagdalena.samariantour.nucleo.pagos.aplicacion;

import co.edu.unimagdalena.samariantour.nucleo.compartido.concurrencia.EjecutorConReintento;
import co.edu.unimagdalena.samariantour.nucleo.pagos.dominio.excepcion.ClaveIdempotenciaReutilizadaException;
import co.edu.unimagdalena.samariantour.nucleo.pagos.dominio.modelo.OperacionRegistrada;
import co.edu.unimagdalena.samariantour.nucleo.pagos.dominio.modelo.PeticionIdempotente;
import co.edu.unimagdalena.samariantour.nucleo.pagos.dominio.modelo.RespuestaGuardada;
import co.edu.unimagdalena.samariantour.nucleo.pagos.dominio.modelo.ResultadoIdempotente;
import co.edu.unimagdalena.samariantour.nucleo.pagos.dominio.puerto.salida.IdempotenciaPort;
import co.edu.unimagdalena.samariantour.nucleo.pagos.dominio.puerto.salida.TransaccionPort;

import java.time.Clock;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * Ejecuta una operación del turista con su clave de idempotencia en una sola transacción, como la de las solicitudes
 * (ADR-04, DN-12, DN-19): reserva la clave, ejecuta la operación y guarda su respuesta con lo que cambió. Si falla,
 * no queda nada y el reintento con la misma clave vuelve a intentarlo. Ante un conflicto de concurrencia se reintenta
 * todo en una transacción nueva (ADR-05).
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

    /** @param operacion puede ejecutarse más de una vez, así que no debe tener efectos fuera de la base */
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
