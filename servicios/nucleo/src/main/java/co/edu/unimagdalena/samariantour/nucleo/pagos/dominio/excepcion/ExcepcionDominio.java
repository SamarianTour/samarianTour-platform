package co.edu.unimagdalena.samariantour.nucleo.pagos.dominio.excepcion;

/**
 * Base de las excepciones de pagos. El código identifica el motivo de forma estable para que los clientes puedan
 * explicarlo, como en el resto del núcleo.
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
