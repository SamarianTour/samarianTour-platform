package co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.excepcion;
/**
 * Base de las excepciones del dominio. El código identifica el motivo de forma estable
 * para que los clientes puedan explicarlo (por ejemplo, RNT_INACTIVO).
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
