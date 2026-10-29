package co.edu.unimagdalena.samariantour.nucleo.pagos.dominio.modelo;

/** Qué hizo la solicitud con un evento del pago, como lo responde {@code solicitudes.api} (DN-17). */
public enum EfectoEnSolicitud {
    APLICADO,
    SIN_CAMBIOS,
    /** Aprobado sobre una solicitud ya vencida o cerrada: no cambió y hay que registrar el pago tardío (RF-50). */
    PAGO_TARDIO
}
