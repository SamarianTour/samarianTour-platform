package co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.excepcion;

/**
 * Solo el prestador dueño de la oferta puede editarla o retirarla.
 */
public class PrestadorNoAutorizadoException extends ExcepcionDominio {

    public PrestadorNoAutorizadoException(String ofertaId) {
        super("PRESTADOR_NO_AUTORIZADO", "El prestador no es el dueño de la oferta %s".formatted(ofertaId));
    }
}
