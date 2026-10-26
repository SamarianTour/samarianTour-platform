package co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.excepcion;

import java.util.UUID;

/**
 * El turista ya tiene una solicitud abierta para la misma oferta, fecha y hora (RF-31). El mensaje dice cuál, para que
 * pueda consultarla.
 */
public class SolicitudDuplicadaException extends ExcepcionDominio {

    private static final String MENSAJE = "Ya tienes una solicitud abierta para esta oferta, fecha y hora";

    /** Detectada por la regla: se sabe cuál es la que ya existe. */
    public SolicitudDuplicadaException(UUID existenteId) {
        super("SOLICITUD_DUPLICADA", MENSAJE + ": " + existenteId);
    }

    /** Detectada por el índice único de la base, cuando dos envíos distintos llegan a la vez. */
    public SolicitudDuplicadaException() {
        super("SOLICITUD_DUPLICADA", MENSAJE);
    }
}
