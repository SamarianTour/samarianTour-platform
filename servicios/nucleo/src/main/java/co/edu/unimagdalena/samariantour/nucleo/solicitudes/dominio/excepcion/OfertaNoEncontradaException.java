package co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.excepcion;

/**
 * El catálogo no tiene esa oferta visible: no existe, está retirada, no tiene versión aprobada o su prestador tiene el
 * RNT inactivo (consulta interna del catálogo, RF-30).
 */
public class OfertaNoEncontradaException extends ExcepcionDominio {

    public OfertaNoEncontradaException(String ofertaId) {
        super("OFERTA_NO_ENCONTRADA", "La oferta %s no existe o no está visible en el catálogo".formatted(ofertaId));
    }
}
