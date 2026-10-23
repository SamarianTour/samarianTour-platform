package co.edu.unimagdalena.samariantour.catalogo.opiniones.dominio.excepcion;

/**
 * Un dato de entrada no cumple las reglas del dominio. Mismo código y mismo formato de mensaje que en el catálogo:
 * "campo: detalle".
 */
public class DatoInvalidoException extends ExcepcionDominio {

    public DatoInvalidoException(String campo, String detalle) {
        super("DATO_INVALIDO", "%s: %s".formatted(campo, detalle));
    }
}
