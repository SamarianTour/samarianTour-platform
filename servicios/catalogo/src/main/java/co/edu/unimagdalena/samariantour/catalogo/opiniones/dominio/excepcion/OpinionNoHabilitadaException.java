package co.edu.unimagdalena.samariantour.catalogo.opiniones.dominio.excepcion;

import java.util.UUID;

/**
 * El turista no tiene una solicitud pagada de la oferta, así que no puede opinar sobre ella (RF-60, DC-41).
 */
public class OpinionNoHabilitadaException extends ExcepcionDominio {

    public OpinionNoHabilitadaException(UUID turistaId, String ofertaId) {
        super("OPINION_NO_HABILITADA",
                "El turista %s no tiene una solicitud pagada de la oferta %s".formatted(turistaId, ofertaId));
    }
}
