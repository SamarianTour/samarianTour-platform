package co.edu.unimagdalena.samariantour.nucleo.bandeja.dominio.excepcion;

/** El aviso no existe o no está rechazado, así que no hay qué reenviar (DN-44). */
public class AvisoNoReenviableException extends RuntimeException {

    public static final String NO_ENCONTRADO = "AVISO_NO_ENCONTRADO";
    public static final String NO_RECHAZADO = "AVISO_NO_RECHAZADO";

    private final String codigo;

    public AvisoNoReenviableException(String codigo, String mensaje) {
        super(mensaje);
        this.codigo = codigo;
    }

    public String codigo() {
        return codigo;
    }
}
