package co.edu.unimagdalena.samariantour.recomendacion.recomendacion.dominio.excepcion;

/**
 * La propuesta del proveedor de IA no cumple {@code ValidacionRespuestaIA}. Se descarta completa (DR-05).
 */
public class RespuestaIAInvalidaException extends RuntimeException {

    public RespuestaIAInvalidaException(String motivo) {
        super(motivo);
    }
}
