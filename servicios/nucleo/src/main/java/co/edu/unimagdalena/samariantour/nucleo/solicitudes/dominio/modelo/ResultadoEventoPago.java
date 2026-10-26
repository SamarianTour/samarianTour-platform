package co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.modelo;

/** Qué hizo la solicitud con un evento del pago (RF-50, RF-55, RF-58). */
public enum ResultadoEventoPago {
    /** Cambió: quedó con el intento en curso, Pagada, con el pago fallido o Vencida. */
    APLICADO,
    /** No había nada que cambiar: el evento ya se había aplicado o llegó a una solicitud cerrada. */
    SIN_CAMBIOS,
    /**
     * Pago aprobado sobre una solicitud que ya venció o se cerró: no la revive. Pagos lo registra como tardío y avisa
     * al administrador para el reembolso manual (RF-50).
     */
    PAGO_TARDIO
}
