package co.edu.unimagdalena.samariantour.nucleo.prestadores.dominio.excepcion;

import co.edu.unimagdalena.samariantour.nucleo.prestadores.dominio.modelo.TipoPrestador;

/**
 * El tipo que declara el prestador no corresponde a su registro en el RNT: por ejemplo, un restaurante cuyo RNT es de
 * alojamiento, o un guía independiente que no es persona natural (RF-68, RF-70, DN-33).
 */
public class TipoNoCoincideConRntException extends ExcepcionDominio {

    public TipoNoCoincideConRntException(TipoPrestador tipo, String categoriaRnt, boolean personaNatural) {
        super("TIPO_NO_COINCIDE_CON_RNT", "El tipo %s no corresponde al RNT, que es de la categoría %s%s"
                .formatted(tipo, categoriaRnt, personaNatural ? " (persona natural)" : " (comerciante)"));
    }
}
