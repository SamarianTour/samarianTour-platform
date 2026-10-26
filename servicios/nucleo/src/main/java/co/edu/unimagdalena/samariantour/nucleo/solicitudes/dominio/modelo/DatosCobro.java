package co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.modelo;

import java.time.Instant;
import java.util.UUID;

/**
 * Lo que pagos necesita para cobrar una solicitud confirmada (RF-46, RF-49): el precio base, que es el monto exacto, y
 * el fin de la ventana.
 */
public record DatosCobro(UUID solicitudId, EstadoSolicitud estado, long precioBase, Instant venceEn,
                         UUID prestadorId, String ofertaId) {
}
