package co.edu.unimagdalena.samariantour.nucleo.administracion.infraestructura.entrada.rest.dto;

import java.time.Instant;
import java.util.UUID;

/**
 * Un pago aprobado que no pagó la solicitud y el administrador revisa y reembolsa a mano, fuera del sistema (RF-49,
 * RF-50).
 *
 * @param monto   lo que aprobó la pasarela, en pesos
 * @param motivo  PAGO_TARDIO (llegó con la solicitud vencida, cerrada o ya pagada) o MONTO_DISTINTO (otro monto que el
 *                del intento)
 * @param avisado si ya se avisó al administrador por WhatsApp
 */
public record PagoTardioResponse(UUID id, UUID solicitudId, String referencia, String transaccionId, long monto,
                                 String motivo, Instant registradoEn, boolean avisado) {
}
