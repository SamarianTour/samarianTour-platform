package co.edu.unimagdalena.samariantour.nucleo.cuentas.infraestructura.entrada.rest.dto;

import java.time.Instant;

/**
 * Los tokens de la sesión (RF-04, DN-29) y la cuenta que la inició.
 *
 * @param tokenAcceso      JWT RS256 que va en {@code Authorization: Bearer}; vence a los 15 minutos
 * @param tokenRenovacion  sirve una sola vez para pedir tokens nuevos; vence a los 7 días sin usarse
 */
public record SesionResponse(String tokenAcceso, String tipoToken, Instant accesoExpiraEn, String tokenRenovacion,
                             Instant renovacionExpiraEn, CuentaResponse cuenta) {

    @Override
    public String toString() {
        return "SesionResponse[cuenta=%s, accesoExpiraEn=%s]".formatted(cuenta.id(), accesoExpiraEn);
    }
}
