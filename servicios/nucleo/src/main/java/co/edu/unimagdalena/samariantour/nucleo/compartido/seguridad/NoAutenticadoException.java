package co.edu.unimagdalena.samariantour.nucleo.compartido.seguridad;

/**
 * La petición necesita la identidad del usuario y no la trae, o la trae mal formada.
 */
public class NoAutenticadoException extends RuntimeException {

    public NoAutenticadoException(String mensaje) {
        super(mensaje);
    }
}
