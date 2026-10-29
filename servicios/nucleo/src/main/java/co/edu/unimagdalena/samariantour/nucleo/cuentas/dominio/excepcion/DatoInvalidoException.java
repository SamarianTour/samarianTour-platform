package co.edu.unimagdalena.samariantour.nucleo.cuentas.dominio.excepcion;

/** Un dato de la cuenta no cumple las reglas (RF-02); el mensaje empieza por el campo, como en el catálogo. */
public class DatoInvalidoException extends ExcepcionDominio {

    public DatoInvalidoException(String campo, String detalle) {
        super("DATO_INVALIDO", "%s: %s".formatted(campo, detalle));
    }
}
