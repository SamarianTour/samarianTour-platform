package co.edu.unimagdalena.samariantour.nucleo.solicitudes.api;

import java.time.Instant;
import java.util.UUID;

/**
 * Lo que pagos necesita para abrir la sesión de cobro (RF-46, RF-49) y para dispersar al prestador (RF-54).
 *
 * @param estado     el estado de la solicitud; solo se paga CONFIRMADA o PAGO_FALLIDO
 * @param precioBase el monto exacto que se cobra, en pesos; vacío si el operador todavía no la atendió
 * @param venceEn    el plazo vigente (RF-55); vacío si está cerrada
 */
public record SolicitudParaCobro(UUID solicitudId, String estado, Long precioBase, Instant venceEn,
                                 UUID prestadorId, String ofertaId) {
}
