package co.edu.unimagdalena.samariantour.nucleo.pagos.dominio.modelo;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Un pago aprobado que no paga nada (RF-50, DN-22): llegó cuando la solicitud ya estaba vencida, cerrada o pagada por
 * otro intento, o es una segunda transacción aprobada del mismo intento. La solicitud no cambia y el administrador lo
 * reembolsa a mano, fuera del sistema.
 *
 * @param avisoEnviadoEn cuándo se avisó al administrador por WhatsApp; vacío mientras no se haya podido
 */
public record PagoTardio(UUID id, UUID solicitudId, UUID intentoId, String referencia, String transaccionId,
                         long montoCentavos, MotivoPagoTardio motivo, Instant registradoEn, Instant avisoEnviadoEn) {

    public PagoTardio {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(solicitudId, "solicitudId");
        Objects.requireNonNull(intentoId, "intentoId");
        Objects.requireNonNull(referencia, "referencia");
        Objects.requireNonNull(transaccionId, "transaccionId");
        Objects.requireNonNull(motivo, "motivo");
        Objects.requireNonNull(registradoEn, "registradoEn");
    }

    /** El de la transacción aprobada {@code transaccionId}, que puede no ser la que resolvió el intento. */
    public static PagoTardio de(UUID id, IntentoPago intento, String transaccionId, Instant ahora) {
        return new PagoTardio(id, intento.solicitudId(), intento.id(), intento.referencia(), transaccionId,
                intento.montoCentavos(), MotivoPagoTardio.PAGO_TARDIO, ahora, null);
    }

    /**
     * Una transacción aprobada por otro monto que el del intento (RF-49): no paga la solicitud, pero el turista pagó.
     *
     * @param montoCobrado lo que aprobó la pasarela, en centavos
     */
    public static PagoTardio montoDistinto(UUID id, IntentoPago intento, String transaccionId, long montoCobrado,
                                           Instant ahora) {
        return new PagoTardio(id, intento.solicitudId(), intento.id(), intento.referencia(), transaccionId,
                montoCobrado, MotivoPagoTardio.MONTO_DISTINTO, ahora, null);
    }

    public PagoTardio avisado(Instant ahora) {
        return new PagoTardio(id, solicitudId, intentoId, referencia, transaccionId, montoCentavos, motivo,
                registradoEn, ahora);
    }
}
