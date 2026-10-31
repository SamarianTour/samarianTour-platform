package co.edu.unimagdalena.samariantour.nucleo.pagos.api;

import java.time.Instant;
import java.util.UUID;

/**
 * Un pago aprobado que no pagó la solicitud y el administrador reembolsa a mano (RF-49, RF-50, DN-22).
 *
 * @param monto   lo que aprobó la pasarela, en pesos
 * @param motivo  PAGO_TARDIO o MONTO_DISTINTO
 * @param avisado si ya se avisó al administrador por WhatsApp
 */
public record PagoTardioRegistrado(UUID id, UUID solicitudId, String referencia, String transaccionId, long monto,
                                   String motivo, Instant registradoEn, boolean avisado) {
}
