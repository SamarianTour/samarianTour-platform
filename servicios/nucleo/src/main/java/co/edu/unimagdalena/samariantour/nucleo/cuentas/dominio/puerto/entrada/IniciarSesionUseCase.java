package co.edu.unimagdalena.samariantour.nucleo.cuentas.dominio.puerto.entrada;

import co.edu.unimagdalena.samariantour.nucleo.cuentas.dominio.excepcion.CredencialesInvalidasException;
import co.edu.unimagdalena.samariantour.nucleo.cuentas.dominio.excepcion.CuentaBloqueadaException;
import co.edu.unimagdalena.samariantour.nucleo.cuentas.dominio.excepcion.CuentaDesactivadaException;
import co.edu.unimagdalena.samariantour.nucleo.cuentas.dominio.modelo.SesionIniciada;

/** Inicia sesión con el correo y la contraseña (RF-04, RF-05). */
public interface IniciarSesionUseCase {

    /**
     * @throws CredencialesInvalidasException si el correo o la contraseña no son correctos; el fallo queda contado
     * @throws CuentaBloqueadaException       si la cuenta está bloqueada, o este fallo fue el quinto consecutivo
     * @throws CuentaDesactivadaException     si la contraseña es correcta pero la cuenta no está activa
     */
    SesionIniciada iniciar(String correo, String contrasena);
}
