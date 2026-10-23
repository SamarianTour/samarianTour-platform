package co.edu.unimagdalena.samariantour.catalogo.opiniones.dominio.excepcion;

/**
 * La oferta no existe, o no es visible para quien lee sus opiniones. Mismo código que en la ficha (DC-15).
 */
public class OfertaNoEncontradaException extends ExcepcionDominio {

    public OfertaNoEncontradaException(String ofertaId) {
        super("OFERTA_NO_ENCONTRADA", "No existe la oferta %s".formatted(ofertaId));
    }
}
