package co.edu.unimagdalena.samariantour.nucleo.pagos.dominio.modelo;

/**
 * Lo que devuelve una operación idempotente (DN-19).
 *
 * @param repetida true si la clave ya se había usado con la misma petición: es la respuesta original
 */
public record ResultadoIdempotente(RespuestaGuardada respuesta, boolean repetida) {
}
