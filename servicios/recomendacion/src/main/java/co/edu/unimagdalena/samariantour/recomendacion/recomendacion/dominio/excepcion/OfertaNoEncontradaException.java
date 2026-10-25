package co.edu.unimagdalena.samariantour.recomendacion.recomendacion.dominio.excepcion;

/**
 * La oferta no está entre las visibles del catálogo: no existe, está retirada o no es visible (DC-36).
 */
public class OfertaNoEncontradaException extends ExcepcionDominio {

    public OfertaNoEncontradaException(String ofertaId) {
        super("OFERTA_NO_ENCONTRADA", "La oferta %s no está entre las visibles del catálogo".formatted(ofertaId));
    }
}
