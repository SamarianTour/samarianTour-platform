package co.edu.unimagdalena.samariantour.nucleo.pagos.infraestructura.entrada.rest.dto;

import co.edu.unimagdalena.samariantour.nucleo.pagos.dominio.modelo.EstadoIntento;

import java.time.Instant;
import java.util.UUID;

/**
 * El intento de pago en curso y cómo abrirlo (RF-46). El monto es el precio base exacto, en centavos (RF-49).
 *
 * @param numero el orden del intento en la solicitud, desde 1
 */
public record SesionCobroResponse(UUID intentoId, UUID solicitudId, int numero, EstadoIntento estado,
                                  String referencia, long montoCentavos, String moneda, Instant creadoEn,
                                  CheckoutResponse checkout) {
}
