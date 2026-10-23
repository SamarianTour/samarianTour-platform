package co.edu.unimagdalena.samariantour.catalogo.opiniones.dominio.excepcion;

import java.util.UUID;

/**
 * El turista ya opinó sobre la oferta: solo se admite una opinión por turista y oferta (RF-61).
 */
public class OpinionDuplicadaException extends ExcepcionDominio {

    public OpinionDuplicadaException(UUID turistaId, String ofertaId) {
        super("OPINION_DUPLICADA", "El turista %s ya opinó sobre la oferta %s".formatted(turistaId, ofertaId));
    }
}
