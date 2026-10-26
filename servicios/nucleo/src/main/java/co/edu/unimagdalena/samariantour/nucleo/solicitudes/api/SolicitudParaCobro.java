package co.edu.unimagdalena.samariantour.nucleo.solicitudes.api;

import java.time.Instant;
import java.util.UUID;

/**
 * Lo que pagos necesita para abrir la sesión de cobro (RF-46, RF-49).
 *
 * @param estado     CONFIRMADA o PAGO_FALLIDO
 * @param precioBase el monto exacto que se cobra, en pesos
 * @param venceEn    el fin de la ventana de confirmación y pago (RF-55)
 */
public record SolicitudParaCobro(UUID solicitudId, String estado, long precioBase, Instant venceEn,
                                 UUID prestadorId, String ofertaId) {
}
