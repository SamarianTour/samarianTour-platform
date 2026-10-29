package co.edu.unimagdalena.samariantour.nucleo.cuentas.dominio.excepcion;

/**
 * El correo o la contraseña no son correctos (RF-04). El mensaje no dice cuál de los dos falló, ni si el correo
 * existe.
 */
public class CredencialesInvalidasException extends ExcepcionDominio {

    public CredencialesInvalidasException() {
        super("CREDENCIALES_INVALIDAS", "El correo o la contraseña no son correctos");
    }
}
