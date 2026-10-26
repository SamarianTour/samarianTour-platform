package co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.excepcion;

/** Un dato de la solicitud o de la atención no cumple las reglas; el mensaje empieza por el campo. */
public class DatoInvalidoException extends ExcepcionDominio {

    public DatoInvalidoException(String campo, String detalle) {
        super("DATO_INVALIDO", "%s: %s".formatted(campo, detalle));
    }
}
