package co.edu.unimagdalena.samariantour.nucleo.cuentas.dominio.puerto.entrada;

import java.util.UUID;

/**
 * La parte de las cuentas en la supresión del prestador (RF-13, DN-45). La orquesta el módulo de prestadores: primero
 * confirma la contraseña y, si no tiene compromisos pendientes, suprime todo en una sola transacción.
 */
public interface SuprimirCuentaPrestadorUseCase {

    /**
     * Confirma con la contraseña que el titular lo pide. Corre en su propia transacción: un fallo queda contado para
     * el bloqueo de RF-05 aunque la supresión no siga.
     *
     * @throws co.edu.unimagdalena.samariantour.nucleo.cuentas.dominio.excepcion.ContrasenaIncorrectaException si no
     *         es correcta
     * @throws co.edu.unimagdalena.samariantour.nucleo.cuentas.dominio.excepcion.CuentaBloqueadaException si está
     *         bloqueada por intentos fallidos
     * @throws co.edu.unimagdalena.samariantour.nucleo.cuentas.dominio.excepcion.CuentaNoEncontradaException si no hay
     *         un prestador activo con ese identificador
     */
    void verificarContrasena(UUID prestadorId, String contrasena);

    /** Anonimiza la cuenta y cierra sus sesiones, en la transacción de quien la llama. */
    void suprimir(UUID prestadorId);
}
