package co.edu.unimagdalena.samariantour.nucleo.pagos.dominio.modelo;

/** El estado de un intento de pago (DN-20). */
public enum EstadoIntento {
    /** El turista abrió el pago y todavía no hay resultado: aplaza el vencimiento de la solicitud (RF-55). */
    EN_CURSO,
    APROBADO,
    /** Rechazado o anulado por la pasarela (RF-58). */
    RECHAZADO,
    /** Error del medio de pago; cuenta como rechazo para RF-58. */
    ERROR,
    /**
     * Sin resultado 30 min después de abrirse [SUPUESTO] (DN-20): deja de aplazar el vencimiento y no cuenta como
     * rechazo. Si después llega su resultado, se aplica igual.
     */
    ABANDONADO;

    /** Si el intento tuvo resultado de la pasarela: son los que cuentan para el límite de tres (RF-58). */
    public boolean tieneResultado() {
        return this == APROBADO || this == RECHAZADO || this == ERROR;
    }
}
