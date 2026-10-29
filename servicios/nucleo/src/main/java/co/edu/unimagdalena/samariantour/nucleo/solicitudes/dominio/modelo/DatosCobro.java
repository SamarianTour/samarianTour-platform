package co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.modelo;

import java.time.Instant;
import java.util.UUID;

/**
 * Lo que pagos necesita para cobrar una solicitud (RF-46, RF-49): su estado, el precio base, que es el monto exacto,
 * y el plazo vigente.
 *
 * @param precioBase vacío si el operador todavía no la atendió
 */
public record DatosCobro(UUID solicitudId, EstadoSolicitud estado, Long precioBase, Instant venceEn,
                         UUID prestadorId, String ofertaId) {
}
