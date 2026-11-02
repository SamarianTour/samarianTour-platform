package co.edu.unimagdalena.samariantour.nucleo.indicadores.dominio.excepcion;

/** Un parámetro de la consulta no cumple las reglas; el mensaje empieza por el campo, como en el catálogo. */
public class DatoInvalidoException extends ExcepcionDominio {

    public DatoInvalidoException(String campo, String detalle) {
        super("DATO_INVALIDO", "%s: %s".formatted(campo, detalle));
    }
}
