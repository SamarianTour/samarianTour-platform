package co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.excepcion;

/**
 * Un dato de entrada no cumple las reglas del dominio.
 */
public class DatoInvalidoException extends ExcepcionDominio {

    public DatoInvalidoException(String campo, String detalle) {
        super("DATO_INVALIDO", "%s: %s".formatted(campo, detalle));
    }
}
