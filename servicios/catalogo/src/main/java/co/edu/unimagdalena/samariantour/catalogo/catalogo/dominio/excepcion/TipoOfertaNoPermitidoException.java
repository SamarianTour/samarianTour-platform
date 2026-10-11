package co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.excepcion;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.TipoOferta;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.TipoPrestador;

/**
 * El tipo de prestador no puede publicar ese tipo de oferta (RF-65).
 */
public class TipoOfertaNoPermitidoException extends ExcepcionDominio {

    public TipoOfertaNoPermitidoException(TipoPrestador tipoPrestador, TipoOferta tipoOferta) {
        super("TIPO_OFERTA_NO_PERMITIDO",
                "Un prestador de tipo %s no puede publicar ofertas de tipo %s".formatted(tipoPrestador, tipoOferta));
    }
}
