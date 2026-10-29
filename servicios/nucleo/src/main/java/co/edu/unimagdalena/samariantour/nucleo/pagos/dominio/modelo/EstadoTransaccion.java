package co.edu.unimagdalena.samariantour.nucleo.pagos.dominio.modelo;

/**
 * El estado de una transacción en la pasarela, como lo informa su evento (documentación de Wompi, "Transacciones"):
 * PENDING mientras se procesa, y uno final.
 */
public enum EstadoTransaccion {
    /** PENDING: todavía no hay resultado. */
    PENDIENTE,
    /** APPROVED. */
    APROBADA,
    /** DECLINED. */
    RECHAZADA,
    /** VOIDED: anulada; solo aplica a tarjetas. */
    ANULADA,
    /** ERROR: error interno del medio de pago. */
    ERROR;

    public boolean esFinal() {
        return this != PENDIENTE;
    }
}
