package co.edu.unimagdalena.samariantour.nucleo.pagos.infraestructura.entrada.rest.dto;

import co.edu.unimagdalena.samariantour.nucleo.pagos.dominio.modelo.EstadoIntento;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.UUID;

/**
 * Un intento de pago de la solicitud, para la reconsulta del turista (ADR-04).
 *
 * @param transaccionId el id de la transacción en la pasarela (RF-52); se omite hasta su primer evento
 * @param resueltoEn    se omite mientras está en curso
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record IntentoPagoResponse(UUID id, int numero, EstadoIntento estado, String referencia, long montoCentavos,
                                  String transaccionId, Instant creadoEn, Instant resueltoEn) {
}
