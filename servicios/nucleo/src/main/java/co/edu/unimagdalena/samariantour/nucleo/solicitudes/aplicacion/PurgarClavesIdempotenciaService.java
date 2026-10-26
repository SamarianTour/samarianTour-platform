package co.edu.unimagdalena.samariantour.nucleo.solicitudes.aplicacion;

import co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.puerto.entrada.PurgarClavesIdempotenciaUseCase;
import co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.puerto.salida.IdempotenciaPort;
import co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.puerto.salida.TransaccionPort;

import java.time.Clock;

/**
 * Borra las claves vencidas (DN-12). Una clave vencida ya no cuenta aunque siga en la tabla, así que la limpieza solo
 * evita que la tabla crezca: si dos réplicas la corren a la vez, la segunda no encuentra nada.
 */
public class PurgarClavesIdempotenciaService implements PurgarClavesIdempotenciaUseCase {

    private final IdempotenciaPort claves;
    private final TransaccionPort transaccion;
    private final Clock reloj;

    public PurgarClavesIdempotenciaService(IdempotenciaPort claves, TransaccionPort transaccion, Clock reloj) {
        this.claves = claves;
        this.transaccion = transaccion;
        this.reloj = reloj;
    }

    @Override
    public int purgar() {
        return transaccion.ejecutar(() -> claves.purgarVencidas(reloj.instant()));
    }
}
