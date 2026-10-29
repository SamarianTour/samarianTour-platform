package co.edu.unimagdalena.samariantour.nucleo.solicitudes.api;

import java.util.Optional;
import java.util.UUID;

/**
 * Lo único que el módulo de pagos usa de las solicitudes (DN-17), como {@code bandeja.api} para los avisos. Se llama
 * desde el adaptador de salida de pagos, dentro de su transacción: los métodos que cambian la solicitud la exigen y no
 * abren una propia, así el pago y la solicitud se confirman juntos. Si otra escritura cambió la solicitud a la vez,
 * lanzan {@code ConflictoConcurrenciaException} (ADR-05) y pagos reintenta su transacción completa.
 *
 * @throws EventoPagoNoAplicableException si la solicitud no existe o su estado no admite el evento
 */
public interface SolicitudesPagosApi {

    /**
     * La solicitud de ese turista, en cualquier estado: pagos decide si la cobra (Confirmada o con el pago fallido).
     * Vacío si no existe o es de otro turista.
     */
    Optional<SolicitudParaCobro> consultarParaCobro(UUID solicitudId, UUID turistaId);

    /** Un intento de pago empezó en la pasarela (RF-55). */
    ResultadoPago marcarIntentoEnCurso(UUID solicitudId);

    /**
     * El intento de pago quedó abandonado, sin resultado de la pasarela (DN-20): deja de aplazar el vencimiento. No
     * cuenta como rechazo.
     */
    ResultadoPago liberarIntentoEnCurso(UUID solicitudId);

    /**
     * El webhook confirmó el pago (RF-50): Pagada, y en la misma transacción se encola el aviso HABILITACION_OPINION al
     * catálogo (DC-41). Sobre una solicitud ya vencida no la revive: responde PAGO_TARDIO.
     */
    ResultadoPago registrarPagoAprobado(UUID solicitudId);

    /** El webhook informó un rechazo (RF-58). */
    ResultadoPago registrarPagoRechazado(UUID solicitudId);
}
