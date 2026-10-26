package co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.puerto.salida;

import co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.modelo.OperacionRegistrada;
import co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.modelo.PeticionIdempotente;
import co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.modelo.RespuestaGuardada;

import java.time.Instant;
import java.util.Optional;

/**
 * Las claves de idempotencia en PostgreSQL, con restricción de unicidad por usuario y clave (ADR-04, DN-12). Todo
 * ocurre en la transacción de la operación.
 */
public interface IdempotenciaPort {

    /**
     * Reserva la clave para esta transacción. Si otra transacción la está usando en ese momento, quizá en la otra
     * réplica, espera a que termine: si confirmó, devuelve lo que registró; si se deshizo, la reserva para esta.
     *
     * @return vacío si la clave quedó reservada para esta operación; la operación registrada si ya se había usado y
     *         sigue vigente
     */
    Optional<OperacionRegistrada> reservar(PeticionIdempotente peticion, Instant ahora);

    /** Guarda la respuesta de la operación que reservó la clave, en su misma transacción. */
    void completar(PeticionIdempotente peticion, RespuestaGuardada respuesta);

    /** Borra las claves vencidas; devuelve cuántas. */
    int purgarVencidas(Instant ahora);
}
