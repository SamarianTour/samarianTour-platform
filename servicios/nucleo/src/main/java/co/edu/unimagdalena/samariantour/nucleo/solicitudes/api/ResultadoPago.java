package co.edu.unimagdalena.samariantour.nucleo.solicitudes.api;

/** Qué hizo la solicitud con un evento del pago (RF-50, RF-55, RF-58). */
public enum ResultadoPago {
    /** Cambió: intento en curso, Pagada, Pago fallido o Vencida. */
    APLICADO,
    /** No había nada que cambiar: el evento ya se había aplicado o llegó a una solicitud cerrada. */
    SIN_CAMBIOS,
    /**
     * Aprobado sobre una solicitud que ya venció o se cerró: no se marca Pagada. Pagos lo registra como tardío y avisa
     * al administrador para el reembolso manual (RF-50).
     */
    PAGO_TARDIO
}
