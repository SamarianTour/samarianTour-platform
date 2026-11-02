package co.edu.unimagdalena.samariantour.nucleo.administracion.dominio.excepcion;

/** El administrador quiso desactivar su propia cuenta: así siempre queda uno activo (DN-40). */
public class CuentaPropiaException extends ExcepcionDominio {

    public CuentaPropiaException() {
        super("CUENTA_PROPIA", "No puedes desactivar tu propia cuenta: pídeselo a otro administrador");
    }
}
