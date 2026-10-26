package co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.modelo;

/** Lo que registra el operador tras verificar con el prestador (RF-39). */
public enum ResultadoAtencion {
    /** Hay cupo: con el precio base, la política de cancelación y el aparte acordado. */
    ATENDIDA,
    /** No hay cupo, o no queda tiempo para confirmar antes del servicio. */
    NO_DISPONIBLE
}
