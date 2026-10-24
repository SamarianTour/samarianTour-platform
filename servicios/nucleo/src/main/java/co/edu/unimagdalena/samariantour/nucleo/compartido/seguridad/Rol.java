package co.edu.unimagdalena.samariantour.nucleo.compartido.seguridad;

/**
 * Los roles del SRS (DN-04). El núcleo los emite en el token; en el prototipo llegan en un header (DN-05). El catálogo
 * solo distingue los tres primeros.
 */
public enum Rol {
    TURISTA,
    PRESTADOR,
    ADMINISTRADOR,
    /** Verifica la disponibilidad con el prestador y atiende la cola de solicitudes (RF-34 a RF-40). */
    OPERADOR_DISPONIBILIDAD,
    /** Consulta los indicadores de la demanda del destino (RF-85 a RF-89). */
    GESTOR_DESTINO
}
