package co.edu.unimagdalena.samariantour.nucleo.pagos.api;

/** El porcentaje de comisión no es válido (RF-82). El mensaje empieza por el nombre del campo. */
public class ComisionInvalidaException extends RuntimeException {

    public ComisionInvalidaException(String mensaje) {
        super(mensaje);
    }
}
