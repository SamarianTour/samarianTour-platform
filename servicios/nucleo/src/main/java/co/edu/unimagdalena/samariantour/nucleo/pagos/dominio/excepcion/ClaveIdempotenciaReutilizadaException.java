package co.edu.unimagdalena.samariantour.nucleo.pagos.dominio.excepcion;

import java.util.UUID;

/** La misma clave de idempotencia llegó con otra petición [SUPUESTO] (DN-12, DN-19): 422. */
public class ClaveIdempotenciaReutilizadaException extends ExcepcionDominio {

    public ClaveIdempotenciaReutilizadaException(UUID clave) {
        super("CLAVE_IDEMPOTENCIA_REUTILIZADA", "La clave de idempotencia " + clave
                + " ya se usó con otra petición: genera una clave nueva para cada operación");
    }
}
