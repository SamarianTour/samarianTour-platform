package co.edu.unimagdalena.samariantour.nucleo.cuentas.dominio.excepcion;

/**
 * La contraseña que confirma una operación delicada, como suprimir la cuenta, no es la correcta (DN-31). El fallo
 * cuenta para el bloqueo de RF-05, igual que en el inicio de sesión.
 */
public class ContrasenaIncorrectaException extends ExcepcionDominio {

    public ContrasenaIncorrectaException() {
        super("CONTRASENA_INCORRECTA", "La contraseña no es correcta");
    }
}
