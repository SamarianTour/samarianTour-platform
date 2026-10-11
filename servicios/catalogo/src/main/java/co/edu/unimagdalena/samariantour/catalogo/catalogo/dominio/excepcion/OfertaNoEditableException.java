package co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.excepcion;

/**
 * La oferta está retirada; el retiro es definitivo (RF-65).
 */
public class OfertaNoEditableException extends ExcepcionDominio {

    public OfertaNoEditableException(String ofertaId) {
        super("OFERTA_NO_EDITABLE", "La oferta %s está retirada y no se puede editar".formatted(ofertaId));
    }
}
