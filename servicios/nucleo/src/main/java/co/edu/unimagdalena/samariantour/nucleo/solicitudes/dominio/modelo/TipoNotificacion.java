package co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.modelo;

/**
 * Los mensajes que produce el ciclo de vida (DN-13). Para WhatsApp cada uno es una plantilla con sus parámetros, como
 * las que exige la API de WhatsApp Business.
 */
public enum TipoNotificacion {
    /** Al turista: su solicitud quedó Pendiente, con el plazo del operador (RF-44). */
    SOLICITUD_RECIBIDA,
    /** Al operador, en la plataforma: llegó una solicitud a su cola (RF-34). */
    NUEVA_SOLICITUD,
    /** Al turista: nombre de la oferta, precio base, plazo y enlace a la confirmación (RF-44, RF-45). */
    SOLICITUD_ATENDIDA,
    /** Al turista (RF-44). */
    SOLICITUD_NO_DISPONIBLE,
    /** Al turista: el operador no la atendió en 24 h (RF-41). */
    SOLICITUD_SIN_RESPUESTA,
    /** Al turista: puede pagar hasta el fin de la ventana (RF-44). */
    SOLICITUD_CONFIRMADA,
    /** Al turista: la declinó y el cupo apartado quedó libre (RF-44, RF-56). */
    SOLICITUD_DECLINADA,
    /** Al turista: venció y el cupo apartado quedó libre (RF-44, RF-56). */
    SOLICITUD_VENCIDA,
    /** Al prestador: el cupo apartado quedó libre porque la solicitud venció o se declinó (RF-56). */
    CUPO_LIBERADO,
    /** Al turista: la pasarela rechazó el pago y puede reintentar dentro de la ventana (RF-44, RF-58). */
    PAGO_FALLIDO,
    /**
     * Al turista, que su solicitud quedó Pagada (RF-44), y al prestador, los datos para coordinar: el nombre del
     * turista, la fecha, la hora, el número de personas y el WhatsApp del turista (RF-57). Es la misma plantilla para
     * los dos: el destinatario decide qué datos lleva. En este proyecto el turista no reserva: envía una solicitud de
     * disponibilidad (glosario del SRS).
     */
    SOLICITUD_PAGADA
}
