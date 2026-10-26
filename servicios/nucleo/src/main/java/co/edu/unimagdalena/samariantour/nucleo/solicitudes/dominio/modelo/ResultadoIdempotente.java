package co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.modelo;

/**
 * Lo que devuelve una operación idempotente (DN-12).
 *
 * @param repetida true si la clave ya se había usado con la misma petición: es la respuesta original y no se hizo nada
 */
public record ResultadoIdempotente(RespuestaGuardada respuesta, boolean repetida) {
}
