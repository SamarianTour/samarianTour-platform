package co.edu.unimagdalena.samariantour.nucleo.pagos.dominio.modelo;

/** Los mensajes de WhatsApp que manda pagos (DN-21, DN-22, DN-24); como las solicitudes, plantillas con sus parámetros. */
public enum TipoAvisoPago {
    /** Al administrador: un pago aprobado llegó tarde y hay que reembolsarlo a mano (RF-50). */
    PAGO_TARDIO,
    /** Al administrador: la pasarela aprobó otro monto que el del intento; no pagó la solicitud y hay que revisarlo. */
    MONTO_DISTINTO,
    /** Al administrador: un evento de la pasarela falló cinco veces al procesarse y hay que revisarlo a mano. */
    EVENTO_FALLIDO,
    /** Al prestador: la dispersión falló tras sus reintentos y debe actualizar su cuenta de cobro (RF-54). */
    DISPERSION_FALLIDA
}
