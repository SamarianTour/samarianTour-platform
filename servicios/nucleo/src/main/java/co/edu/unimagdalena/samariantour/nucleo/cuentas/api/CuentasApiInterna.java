package co.edu.unimagdalena.samariantour.nucleo.cuentas.api;

import java.util.Optional;
import java.util.UUID;

/**
 * Lo único que los demás módulos usan de las cuentas (DN-17, DN-32), como {@code solicitudes.api} para pagos. Las
 * cuentas no dependen de ningún módulo: son los demás los que las consultan por aquí.
 */
public interface CuentasApiInterna {

    /**
     * El nombre y el WhatsApp del turista, para notificarle (RF-44) y para el prestador cuando la solicitud queda Pagada
     * (RF-57). Solo si la cuenta está activa y el WhatsApp verificado (RF-03, RF-30); si no, vacío.
     */
    Optional<ContactoTurista> contactoTurista(UUID turistaId);
}
