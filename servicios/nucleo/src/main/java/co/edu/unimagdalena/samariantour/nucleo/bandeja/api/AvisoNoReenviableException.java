package co.edu.unimagdalena.samariantour.nucleo.bandeja.api;

/**
 * El aviso no se puede reenviar (DN-44).
 *
 * @see #codigo() {@code AVISO_NO_ENCONTRADO} o {@code AVISO_NO_RECHAZADO}
 */
public class AvisoNoReenviableException extends RuntimeException {

    private final String codigo;

    public AvisoNoReenviableException(String codigo, String mensaje) {
        super(mensaje);
        this.codigo = codigo;
    }

    public String codigo() {
        return codigo;
    }
}
