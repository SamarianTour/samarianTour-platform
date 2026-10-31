package co.edu.unimagdalena.samariantour.nucleo.pagos.dominio.modelo;

/** En qué va el procesamiento de un evento de la pasarela (DN-21). */
public enum EstadoProcesamiento {
    /** En la cola: todavía no se procesó, o falló y espera su próximo intento. */
    PENDIENTE,
    /** Procesado, con su resultado. */
    PROCESADO,
    /** Falló cinco veces: salió de la cola y el administrador lo revisa a mano [SUPUESTO]. */
    FALLIDO
}
