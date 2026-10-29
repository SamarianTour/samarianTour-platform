package co.edu.unimagdalena.samariantour.nucleo.pagos.dominio.modelo;

/** Qué cambió en un intento con un evento de la pasarela, y qué hay que llevar a la solicitud. */
public enum ResultadoIntento {
    /** Nada que llevar a la solicitud: un estado no final, o un intento que ya estaba resuelto. */
    SIN_CAMBIOS,
    /** Aprobado: la solicitud pasa a Pagada (RF-50). */
    APROBADO,
    /** Rechazado, anulado o con error: Pago fallido, o Vencida al tercero (RF-58). */
    RECHAZADO,
    /**
     * Aprobado por otra transacción cuando el intento ya tenía resultado: el turista reintentó con la misma referencia.
     * El dinero no paga nada y queda como pago tardío para el reembolso manual (RF-50, DN-22).
     */
    APROBADO_SIN_EFECTO
}
