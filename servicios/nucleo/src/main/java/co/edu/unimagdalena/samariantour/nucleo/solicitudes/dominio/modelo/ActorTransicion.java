package co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.modelo;

/** Quién provocó un cambio de estado (TransicionEstado del diccionario de clases). */
public enum ActorTransicion {
    TURISTA,
    OPERADOR,
    /** Los vencimientos automáticos (RF-41, RF-55) y el cierre por el RNT inactivo del prestador (RF-77). */
    SISTEMA,
    /** Los eventos de la pasarela, que llegan por el módulo de pagos (RF-50, RF-58). */
    PAGOS
}
