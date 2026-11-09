package co.edu.unimagdalena.samariantour.nucleo.pagos.dominio.puerto.entrada;

import java.util.UUID;

/**
 * El prestador cambió su cuenta de cobro: sus dispersiones fallidas vuelven a quedar pendientes para enviarse a la
 * cuenta nueva (RF-54, DN-44). Corre en la transacción del cambio de cuenta.
 */
public interface ReactivarDispersionesUseCase {

    /** @return cuántas reactivó */
    int reactivar(UUID prestadorId);
}
