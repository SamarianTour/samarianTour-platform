package co.edu.unimagdalena.samariantour.nucleo.cuentas.dominio.puerto.entrada.comando;

import java.util.UUID;

/**
 * Los datos personales que se reemplazan (RF-12).
 *
 * @param usuarioId el de la identidad, nunca uno del cuerpo
 * @param whatsapp  obligatorio para el turista; el prestador no lo envía
 */
public record ActualizarDatosCommand(UUID usuarioId, String nombre, String whatsapp) {
}
