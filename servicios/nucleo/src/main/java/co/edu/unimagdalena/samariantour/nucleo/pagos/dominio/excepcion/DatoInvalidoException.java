package co.edu.unimagdalena.samariantour.nucleo.pagos.dominio.excepcion;

/** Un dato de la petición no cumple sus reglas. El mensaje empieza por el nombre del campo. */
public class DatoInvalidoException extends ExcepcionDominio {

    public DatoInvalidoException(String campo, String motivo) {
        super("DATO_INVALIDO", campo + ": " + motivo);
    }
}
