package co.edu.unimagdalena.samariantour.nucleo.pagos.dominio.modelo;

/** Por qué un pago aprobado quedó para que el administrador lo revise y lo reembolse a mano (DN-22). */
public enum MotivoPagoTardio {
    /** Llegó cuando la solicitud ya estaba vencida, cerrada o pagada, o es otra transacción del mismo intento (RF-50). */
    PAGO_TARDIO,
    /** La pasarela aprobó otro monto que el del intento: no paga la solicitud, pero el turista sí pagó (RF-49). */
    MONTO_DISTINTO
}
