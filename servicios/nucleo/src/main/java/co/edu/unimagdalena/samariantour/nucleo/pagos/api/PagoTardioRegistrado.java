package co.edu.unimagdalena.samariantour.nucleo.pagos.api;

import java.time.Instant;
import java.util.UUID;

/**
 * Un pago aprobado que llegó cuando la solicitud ya estaba vencida o cerrada (RF-50).
 *
 * @param monto   en pesos
 * @param avisado si ya se avisó al administrador por WhatsApp
 */
public record PagoTardioRegistrado(UUID id, UUID solicitudId, String referencia, String transaccionId, long monto,
                                   Instant registradoEn, boolean avisado) {
}
