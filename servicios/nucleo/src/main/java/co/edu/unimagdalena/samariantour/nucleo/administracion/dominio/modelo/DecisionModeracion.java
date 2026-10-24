package co.edu.unimagdalena.samariantour.nucleo.administracion.dominio.modelo;

import co.edu.unimagdalena.samariantour.nucleo.administracion.dominio.excepcion.DatoInvalidoException;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Objects;
import java.util.UUID;

/**
 * Lo que decidió un administrador sobre una versión de una oferta (RF-79) o sobre una opinión (RF-80). Queda como
 * historial y como el aviso que se envía al catálogo (DN-08).
 *
 * @param numeroVersion la versión de la oferta que se decide; vacío para una opinión, que no tiene versiones
 * @param motivo        obligatorio al rechazar, como lo exige el catálogo; se notifica a quien publicó
 * @param idAviso       identificador estable del aviso: decidir de nuevo lo mismo da el mismo identificador
 */
public record DecisionModeracion(
        TipoModerado tipo,
        String moderadoId,
        Integer numeroVersion,
        Decision decision,
        String motivo,
        UUID administradorId,
        Instant decididaEn,
        String idAviso) {

    public static final int LONGITUD_MAXIMA_MOTIVO = 1000;

    public DecisionModeracion {
        Objects.requireNonNull(tipo, "tipo");
        Objects.requireNonNull(moderadoId, "moderadoId");
        Objects.requireNonNull(decision, "decision");
        Objects.requireNonNull(administradorId, "administradorId");
        Objects.requireNonNull(decididaEn, "decididaEn");
        Objects.requireNonNull(idAviso, "idAviso");
        if ((tipo == TipoModerado.OFERTA) != (numeroVersion != null)) {
            throw new IllegalArgumentException("Solo la decisión sobre una oferta lleva número de versión");
        }
        // Con la precisión de PostgreSQL: la decisión recién tomada y la leída después responden la misma fecha.
        decididaEn = decididaEn.truncatedTo(ChronoUnit.MICROS);
        motivo = motivo == null || motivo.isBlank() ? null : motivo.strip();
        if (decision == Decision.RECHAZAR && motivo == null) {
            throw new DatoInvalidoException("motivo", "es obligatorio al rechazar");
        }
        if (motivo != null && motivo.length() > LONGITUD_MAXIMA_MOTIVO) {
            throw new DatoInvalidoException("motivo",
                    "tiene como máximo %d caracteres".formatted(LONGITUD_MAXIMA_MOTIVO));
        }
    }

    /** La decisión sobre la versión en moderación de una oferta. */
    public static DecisionModeracion deOferta(String ofertaId, int numeroVersion, Decision decision, String motivo,
                                              UUID administradorId, Instant ahora) {
        return new DecisionModeracion(TipoModerado.OFERTA, ofertaId, numeroVersion, decision, motivo,
                administradorId, ahora, "moderacion-oferta-%s-v%d".formatted(ofertaId, numeroVersion));
    }

    /** La decisión sobre una opinión en moderación. */
    public static DecisionModeracion deOpinion(String opinionId, Decision decision, String motivo,
                                               UUID administradorId, Instant ahora) {
        return new DecisionModeracion(TipoModerado.OPINION, opinionId, null, decision, motivo, administradorId,
                ahora, "moderacion-opinion-" + opinionId);
    }

    /**
     * Si otra decisión sobre lo mismo dice lo mismo: aprobar y aprobar, o rechazar y rechazar, aunque el motivo
     * cambie. Repetirla no es un error (DN-08).
     */
    public boolean coincideCon(DecisionModeracion otra) {
        return decision == otra.decision;
    }
}
