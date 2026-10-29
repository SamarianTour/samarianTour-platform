package co.edu.unimagdalena.samariantour.nucleo.cuentas.dominio.excepcion;

/**
 * La contraseña es correcta pero la cuenta no está activa: el administrador la dio de baja (RF-81). Solo se dice
 * después de verificar la contraseña, para no revelar el estado de una cuenta ajena.
 */
public class CuentaDesactivadaException extends ExcepcionDominio {

    public CuentaDesactivadaException() {
        super("CUENTA_DESACTIVADA", "La cuenta está desactivada");
    }
}
