package co.edu.unimagdalena.samariantour.catalogo.opiniones.dominio.modelo;

/**
 * Estado de moderación de una opinión (RF-61, RF-80). Solo las aprobadas se publican.
 */
public enum EstadoOpinion {
    EN_MODERACION,
    APROBADA,
    RECHAZADA
}
