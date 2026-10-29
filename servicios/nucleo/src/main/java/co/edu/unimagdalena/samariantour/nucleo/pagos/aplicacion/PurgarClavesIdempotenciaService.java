package co.edu.unimagdalena.samariantour.nucleo.pagos.aplicacion;

import co.edu.unimagdalena.samariantour.nucleo.pagos.dominio.puerto.entrada.PurgarClavesIdempotenciaUseCase;
import co.edu.unimagdalena.samariantour.nucleo.pagos.dominio.puerto.salida.IdempotenciaPort;
import co.edu.unimagdalena.samariantour.nucleo.pagos.dominio.puerto.salida.TransaccionPort;

import java.time.Clock;

/** Borra las claves de idempotencia de pagos vencidas (DN-19). */
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
