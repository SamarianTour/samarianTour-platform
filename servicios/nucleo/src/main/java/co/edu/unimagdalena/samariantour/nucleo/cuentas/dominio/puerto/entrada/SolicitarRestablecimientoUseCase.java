package co.edu.unimagdalena.samariantour.nucleo.cuentas.dominio.puerto.entrada;

import co.edu.unimagdalena.samariantour.nucleo.cuentas.dominio.excepcion.CorreoNoDisponibleException;

/** Quien olvidó la contraseña pide un enlace para ponerle otra, al correo registrado (RF-07). */
public interface SolicitarRestablecimientoUseCase {

    /**
     * Si el correo es de una cuenta activa, le envía el enlace; si no, no hace nada y tampoco lo dice, para no revelar
     * qué correos están registrados. Un enlace nuevo invalida los anteriores.
     *
     * @throws CorreoNoDisponibleException si el servidor de correo no responde
     */
    void solicitar(String correo);
}
