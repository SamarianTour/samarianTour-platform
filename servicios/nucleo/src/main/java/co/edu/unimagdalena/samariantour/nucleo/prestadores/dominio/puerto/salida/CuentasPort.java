package co.edu.unimagdalena.samariantour.nucleo.prestadores.dominio.puerto.salida;

import java.util.UUID;

/** Lo que los prestadores necesitan de las cuentas (módulo 1): que la cuenta exista y sea de un prestador activo. */
public interface CuentasPort {

    boolean esPrestadorActivo(UUID usuarioId);

    /**
     * Confirma con la contraseña que el prestador pide suprimir sus datos (DN-45). En su propia transacción: un fallo
     * queda contado para el bloqueo aunque la supresión no siga.
     *
     * @throws co.edu.unimagdalena.samariantour.nucleo.prestadores.dominio.excepcion.ContrasenaRechazadaException si
     *         no la confirma
     */
    void verificarContrasena(UUID prestadorId, String contrasena);

    /** Anonimiza su cuenta y cierra sus sesiones, en la transacción de la supresión. */
    void suprimirCuenta(UUID prestadorId);
}
