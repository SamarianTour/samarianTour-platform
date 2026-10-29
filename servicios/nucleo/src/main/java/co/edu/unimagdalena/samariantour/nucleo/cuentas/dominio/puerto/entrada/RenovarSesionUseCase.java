package co.edu.unimagdalena.samariantour.nucleo.cuentas.dominio.puerto.entrada;

import co.edu.unimagdalena.samariantour.nucleo.cuentas.dominio.excepcion.SesionInvalidaException;
import co.edu.unimagdalena.samariantour.nucleo.cuentas.dominio.modelo.SesionIniciada;

/** Da un token de acceso nuevo sin pedir la contraseña, con el token de renovación (RF-04, DN-19). */
public interface RenovarSesionUseCase {

    /**
     * Cambia también el token de renovación: el usado ya no sirve. Si alguien presenta uno que ya se usó, la sesión
     * se cierra, porque es señal de que el token se filtró.
     *
     * @throws SesionInvalidaException si el token no sirve; hay que iniciar sesión de nuevo
     */
    SesionIniciada renovar(String tokenRenovacion);
}
