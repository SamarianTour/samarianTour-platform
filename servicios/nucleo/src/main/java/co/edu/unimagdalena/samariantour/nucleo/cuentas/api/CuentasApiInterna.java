package co.edu.unimagdalena.samariantour.nucleo.cuentas.api;

import java.util.Optional;
import java.util.UUID;

/**
 * Lo único que los demás módulos usan de las cuentas (DN-17, DN-32), como {@code solicitudes.api} para pagos. Las
 * cuentas no dependen de ningún módulo: son los demás los que las consultan por aquí.
 */
public interface CuentasApiInterna {

    /**
     * El nombre y el WhatsApp del turista, para notificarle (RF-44) y para el prestador cuando la solicitud queda
     * Pagada (RF-57). Solo si la cuenta está activa y el WhatsApp verificado (RF-03, RF-30); si no, vacío.
     */
    Optional<ContactoTurista> contactoTurista(UUID turistaId);

    /** Si la cuenta existe, es de un prestador y está activa; la usa el registro del prestador (RF-62). */
    boolean esPrestadorActivo(UUID usuarioId);

    /**
     * Confirma con la contraseña que el prestador pide suprimir sus datos (RF-13, DN-45). En su propia transacción:
     * un fallo queda contado para el bloqueo de RF-05 aunque la supresión no siga.
     *
     * @throws ContrasenaRechazadaException si no es correcta, la cuenta está bloqueada o no es de un prestador activo
     */
    void verificarContrasenaPrestador(UUID prestadorId, String contrasena);

    /** Anonimiza la cuenta del prestador y cierra sus sesiones, en la transacción de quien la llama (DN-45). */
    void suprimirCuentaPrestador(UUID prestadorId);
}
