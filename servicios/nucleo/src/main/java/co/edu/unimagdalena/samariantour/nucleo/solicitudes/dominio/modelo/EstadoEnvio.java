package co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.modelo;

/** El envío de una notificación (EstadoEnvio del diccionario de clases). */
public enum EstadoEnvio {
    PENDIENTE,
    ENVIADA,
    /** Se agotaron los intentos; queda registrada para revisarla. */
    FALLIDA
}
