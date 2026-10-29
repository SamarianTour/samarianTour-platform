package co.edu.unimagdalena.samariantour.nucleo.pagos.dominio.modelo;

/** El estado de una dispersión al prestador (RF-54, DN-24). */
public enum EstadoDispersion {
    /** Por ejecutar, o esperando su próximo reintento. */
    PENDIENTE,
    EXITOSA,
    /** Falló el primer envío y sus tres reintentos: se avisa al prestador para que actualice su cuenta (RF-54). */
    FALLIDA
}
