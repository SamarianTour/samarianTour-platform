package co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.modelo;

/**
 * Los nueve estados del ciclo de vida de una solicitud (SRS 3.1.3, RF-33, DN-09). Las transiciones válidas viven en
 * {@link SolicitudDisponibilidad}.
 */
public enum EstadoSolicitud {
    /** Recién enviada, en la cola del operador: 24 h para atenderla (RF-41). */
    PENDIENTE(false),
    /** El operador registró precio y condiciones; corre la ventana de confirmación y pago (RF-55). */
    ATENDIDA(false),
    /** El prestador no tiene cupo, o no queda tiempo para confirmar antes del servicio (RF-39). */
    NO_DISPONIBLE(true),
    /** El operador no la atendió en 24 h (RF-41). */
    SIN_RESPUESTA(true),
    /** El turista aceptó las condiciones y puede pagar (RF-43). */
    CONFIRMADA(false),
    /** El turista rechazó las condiciones (RF-43). */
    DECLINADA(true),
    /** La pasarela rechazó un intento de pago; quedan reintentos dentro de la ventana (RF-58). */
    PAGO_FALLIDO(false),
    /** El pago se confirmó por el webhook (RF-50). */
    PAGADA(true),
    /** Terminó la ventana sin pago, o se rechazó el tercer intento (RF-55, RF-58). */
    VENCIDA(true);

    private final boolean terminal;

    EstadoSolicitud(boolean terminal) {
        this.terminal = terminal;
    }

    public boolean esTerminal() {
        return terminal;
    }
}
