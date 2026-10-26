package co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.modelo;

import java.util.Objects;

/**
 * La respuesta de una operación idempotente tal como se envió: el código HTTP y el cuerpo ya convertido (DN-12). La
 * aplicación no la interpreta; la arma y la usa la entrada REST.
 */
public record RespuestaGuardada(int codigo, String cuerpo) {

    public RespuestaGuardada {
        Objects.requireNonNull(cuerpo, "cuerpo");
    }
}
