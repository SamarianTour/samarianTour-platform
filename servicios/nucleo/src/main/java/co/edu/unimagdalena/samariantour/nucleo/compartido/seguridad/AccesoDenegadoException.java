package co.edu.unimagdalena.samariantour.nucleo.compartido.seguridad;

/**
 * El usuario está identificado, pero su rol no puede usar la ruta: por ejemplo, un turista que llama a
 * {@code /admin/**}.
 */
public class AccesoDenegadoException extends RuntimeException {

    public AccesoDenegadoException(String mensaje) {
        super(mensaje);
    }
}
