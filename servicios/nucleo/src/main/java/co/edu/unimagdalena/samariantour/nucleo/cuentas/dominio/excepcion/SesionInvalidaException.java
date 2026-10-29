package co.edu.unimagdalena.samariantour.nucleo.cuentas.dominio.excepcion;

/**
 * El token de renovación no sirve: no existe, venció, la sesión se cerró o ya se había usado (DN-19). Hay que volver
 * a iniciar sesión.
 */
public class SesionInvalidaException extends ExcepcionDominio {

    public SesionInvalidaException() {
        super("SESION_INVALIDA", "La sesión venció o se cerró: inicia sesión de nuevo");
    }
}
