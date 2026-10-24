package co.edu.unimagdalena.samariantour.recomendacion.recomendacion.dominio.excepcion;

/**
 * Base de las excepciones que llegan al cliente. El código identifica el motivo de forma estable para que el cliente
 * pueda explicarlo (por ejemplo, RECOMENDACION_NO_DISPONIBLE).
 */
public abstract class ExcepcionDominio extends RuntimeException {

    private final String codigo;

    protected ExcepcionDominio(String codigo, String mensaje) {
        super(mensaje);
        this.codigo = codigo;
    }

    public String codigo() {
        return codigo;
    }
}
