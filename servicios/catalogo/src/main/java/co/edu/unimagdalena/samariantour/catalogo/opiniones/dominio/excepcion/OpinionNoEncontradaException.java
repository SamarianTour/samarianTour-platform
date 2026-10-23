package co.edu.unimagdalena.samariantour.catalogo.opiniones.dominio.excepcion;

/**
 * La opinión no existe, o no está en el estado que pide la consulta (por ejemplo, ya no espera moderación).
 */
public class OpinionNoEncontradaException extends ExcepcionDominio {

    public OpinionNoEncontradaException(String opinionId) {
        super("OPINION_NO_ENCONTRADA", "No existe la opinión %s".formatted(opinionId));
    }
}
