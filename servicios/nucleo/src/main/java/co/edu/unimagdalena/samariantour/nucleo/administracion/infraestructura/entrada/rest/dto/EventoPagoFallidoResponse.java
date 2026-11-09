package co.edu.unimagdalena.samariantour.nucleo.administracion.infraestructura.entrada.rest.dto;

import java.time.Instant;
import java.util.UUID;

/** Un evento de la pasarela que falló al procesarse (DN-21, DN-44). El monto va en pesos. */
public record EventoPagoFallidoResponse(UUID id, String referencia, String transaccionId, String estado, long monto,
                                        int intentos, String ultimoError, Instant recibidoEn) {
}
