package co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.excepcion;

import java.util.UUID;

/**
 * La clave de idempotencia ya se usó con otra petición [SUPUESTO] (DN-12): el cliente reutilizó la clave de una
 * operación distinta. No se hace nada; cada operación nueva necesita su propia clave.
 */
public class ClaveIdempotenciaReutilizadaException extends ExcepcionDominio {

    public ClaveIdempotenciaReutilizadaException(UUID clave) {
        super("CLAVE_IDEMPOTENCIA_REUTILIZADA", ("La clave de idempotencia %s ya se usó con otra petición: genera una "
                + "clave nueva para cada operación").formatted(clave));
    }
}
