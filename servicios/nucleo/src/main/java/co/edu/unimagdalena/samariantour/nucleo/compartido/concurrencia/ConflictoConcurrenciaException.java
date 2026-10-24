package co.edu.unimagdalena.samariantour.nucleo.compartido.concurrencia;

/**
 * Otra escritura cambió el dato entre la lectura y el guardado (ADR-05). La lanza un adaptador de salida;
 * {@link EjecutorConReintento} vuelve a ejecutar el caso de uso completo y, si se agotan los intentos, la API
 * responde 409 {@code CONFLICTO_CONCURRENCIA}.
 */
public class ConflictoConcurrenciaException extends RuntimeException {

    public static final String CODIGO = "CONFLICTO_CONCURRENCIA";

    public ConflictoConcurrenciaException(String mensaje) {
        super(mensaje);
    }
}
