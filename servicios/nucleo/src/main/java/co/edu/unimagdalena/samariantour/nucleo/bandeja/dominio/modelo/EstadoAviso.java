package co.edu.unimagdalena.samariantour.nucleo.bandeja.dominio.modelo;

public enum EstadoAviso {
    /** Espera su próximo intento. */
    PENDIENTE,
    /** El catálogo lo recibió. */
    ENVIADO,
    /** El catálogo lo rechazó por mal formado; reintentar no lo arregla. */
    RECHAZADO
}
