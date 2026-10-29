package co.edu.unimagdalena.samariantour.nucleo.pagos.dominio.modelo;

/** Los mensajes de WhatsApp que manda pagos (DN-22, DN-24); como las solicitudes, plantillas con sus parámetros. */
public enum TipoAvisoPago {
    /** Al administrador: un pago aprobado llegó tarde y hay que reembolsarlo a mano (RF-50). */
    PAGO_TARDIO,
    /** Al prestador: la dispersión falló tras sus reintentos y debe actualizar su cuenta de cobro (RF-54). */
    DISPERSION_FALLIDA
}
