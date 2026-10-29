package co.edu.unimagdalena.samariantour.nucleo.prestadores.dominio.excepcion;

/** Un dato del prestador no cumple las reglas; el mensaje empieza por el campo, como en el resto del núcleo. */
public class DatoInvalidoException extends ExcepcionDominio {

    public DatoInvalidoException(String campo, String detalle) {
        super("DATO_INVALIDO", "%s: %s".formatted(campo, detalle));
    }
}
