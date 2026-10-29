package co.edu.unimagdalena.samariantour.nucleo.pagos.dominio.modelo;

import java.util.Objects;
import java.util.UUID;

/**
 * Lo que se le pide a la pasarela para transferir al prestador (RF-54). La referencia es el id de la dispersión, para
 * que la pasarela no la repita si se reenvía la misma.
 *
 * @param montoNeto en pesos
 */
public record OrdenDispersion(UUID dispersionId, UUID prestadorId, long montoNeto, CuentaCobro cuenta) {

    public OrdenDispersion {
        Objects.requireNonNull(dispersionId, "dispersionId");
        Objects.requireNonNull(prestadorId, "prestadorId");
        Objects.requireNonNull(cuenta, "cuenta");
    }
}
