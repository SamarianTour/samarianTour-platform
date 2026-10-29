package co.edu.unimagdalena.samariantour.nucleo.cuentas.dominio.puerto.entrada;

import co.edu.unimagdalena.samariantour.nucleo.cuentas.dominio.excepcion.CodigoNoVigenteException;
import co.edu.unimagdalena.samariantour.nucleo.cuentas.dominio.excepcion.DatoInvalidoException;

/** Pone la contraseña nueva con el enlace del correo (RF-08, y RF-81 para la primera de una cuenta interna). */
public interface RestablecerContrasenaUseCase {

    /**
     * Gasta el enlace, cambia la contraseña, quita el bloqueo por intentos y cierra todas las sesiones abiertas.
     *
     * @throws DatoInvalidoException    si la contraseña nueva no cumple las reglas; el enlace sigue sirviendo
     * @throws CodigoNoVigenteException si el enlace no existe, venció, ya se usó o se pidió uno más reciente
     */
    void restablecer(String token, String contrasenaNueva);
}
