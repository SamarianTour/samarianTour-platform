package co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.excepcion;

/**
 * La oferta no existe o no es visible para quien la consulta.
 */
public class OfertaNoEncontradaException extends ExcepcionDominio {

    public OfertaNoEncontradaException(String ofertaId) {
        super("OFERTA_NO_ENCONTRADA", "No existe la oferta %s".formatted(ofertaId));
    }
}
