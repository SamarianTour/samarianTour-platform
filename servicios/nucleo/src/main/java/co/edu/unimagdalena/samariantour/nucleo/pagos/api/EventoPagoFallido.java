package co.edu.unimagdalena.samariantour.nucleo.pagos.api;

import java.time.Instant;
import java.util.UUID;

/**
 * Un evento de la pasarela que falló al procesarse y espera revisión (DN-21, DN-44).
 *
 * @param estado      el estado de la transacción en la pasarela (APPROVED, DECLINED, ...)
 * @param monto       en pesos
 * @param intentos    los fallos al procesarlo
 * @param ultimoError el tipo y el mensaje del último fallo
 */
public record EventoPagoFallido(UUID id, String referencia, String transaccionId, String estado, long monto,
                                int intentos, String ultimoError, Instant recibidoEn) {
}
