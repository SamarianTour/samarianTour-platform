package co.edu.unimagdalena.samariantour.nucleo.cuentas.dominio.puerto.salida;

import co.edu.unimagdalena.samariantour.nucleo.cuentas.dominio.excepcion.CorreoNoDisponibleException;

/** Los correos de las cuentas (RF-07, RF-81). */
public interface EmailPort {

    /**
     * El enlace para poner una contraseña nueva; vence a los 30 minutos y sirve una vez (RF-07, RF-08).
     *
     * @param token el secreto del enlace, {@code <codigoId>.<secreto>}; el adaptador arma la dirección
     * @throws CorreoNoDisponibleException si el servidor de correo no responde
     */
    void enviarRestablecimiento(String correo, String nombre, String token);
}
