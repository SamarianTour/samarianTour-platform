package co.edu.unimagdalena.samariantour.nucleo.pagos.api;

/**
 * El evento no se puede reintentar (DN-44).
 *
 * @see #codigo() {@code EVENTO_NO_ENCONTRADO} o {@code EVENTO_NO_FALLIDO}
 */
public class EventoNoReintentableException extends RuntimeException {

    private final String codigo;

    public EventoNoReintentableException(String codigo, String mensaje) {
        super(mensaje);
        this.codigo = codigo;
    }

    public String codigo() {
        return codigo;
    }
}
