package co.edu.unimagdalena.samariantour.nucleo.pagos.dominio.puerto.salida;

import co.edu.unimagdalena.samariantour.nucleo.pagos.dominio.modelo.OperacionRegistrada;
import co.edu.unimagdalena.samariantour.nucleo.pagos.dominio.modelo.PeticionIdempotente;
import co.edu.unimagdalena.samariantour.nucleo.pagos.dominio.modelo.RespuestaGuardada;

import java.time.Instant;
import java.util.Optional;

/** Las claves de idempotencia de pagos en PostgreSQL, como las de las solicitudes (DN-12, DN-19). */
public interface IdempotenciaPort {

    /**
     * Reserva la clave para esta transacción; si otra la está usando, espera a que termine.
     *
     * @return vacío si quedó reservada; la operación registrada si ya se había usado y sigue vigente
     */
    Optional<OperacionRegistrada> reservar(PeticionIdempotente peticion, Instant ahora);

    void completar(PeticionIdempotente peticion, RespuestaGuardada respuesta);

    int purgarVencidas(Instant ahora);
}
