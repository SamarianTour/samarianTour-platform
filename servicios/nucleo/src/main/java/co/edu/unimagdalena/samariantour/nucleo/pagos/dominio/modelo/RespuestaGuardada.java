package co.edu.unimagdalena.samariantour.nucleo.pagos.dominio.modelo;

import java.util.Objects;

/** La respuesta de una operación idempotente tal como se envió: el código HTTP y el cuerpo ya convertido (DN-19). */
public record RespuestaGuardada(int codigo, String cuerpo) {

    public RespuestaGuardada {
        Objects.requireNonNull(cuerpo, "cuerpo");
    }
}
