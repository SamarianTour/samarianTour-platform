package co.edu.unimagdalena.samariantour.catalogo.opiniones.infraestructura.salida.persistencia.mapper;

import co.edu.unimagdalena.samariantour.catalogo.opiniones.dominio.modelo.HabilitacionOpinion;
import co.edu.unimagdalena.samariantour.catalogo.opiniones.dominio.modelo.Opinion;
import co.edu.unimagdalena.samariantour.catalogo.opiniones.infraestructura.salida.persistencia.document.HabilitacionOpinionDocument;
import co.edu.unimagdalena.samariantour.catalogo.opiniones.infraestructura.salida.persistencia.document.OpinionDocument;
import org.bson.types.ObjectId;

import java.util.Optional;

/**
 * Convierte entre las opiniones y habilitaciones del dominio y sus documentos.
 */
public final class OpinionMongoMapper {

    private OpinionMongoMapper() {
    }

    /** @param revision la revisión con la que queda guardada */
    public static OpinionDocument aDocumento(Opinion opinion, long revision) {
        return new OpinionDocument(referencia(opinion.id()), referencia(opinion.ofertaId()), opinion.turistaId(),
                opinion.calificacion(), opinion.comentario(), opinion.fecha(), opinion.estado(),
                opinion.motivoRechazo(), opinion.revisadaEn(), revision);
    }

    public static Opinion aDominio(OpinionDocument documento) {
        return Opinion.reconstituir(documento.id().toHexString(), documento.ofertaId().toHexString(),
                documento.turistaId(), documento.calificacion(), documento.comentario(), documento.fecha(),
                documento.estado(), documento.motivoRechazo(), documento.revisadaEn(), documento.revision());
    }

    public static HabilitacionOpinionDocument aDocumento(HabilitacionOpinion habilitacion) {
        return new HabilitacionOpinionDocument(null, habilitacion.turistaId(), referencia(habilitacion.ofertaId()));
    }

    /**
     * El identificador como ObjectId, o vacío si no tiene ese formato: un id así no puede existir en la base, y quien
     * lo busca lo trata como inexistente (DC-30).
     */
    public static Optional<ObjectId> objectId(String id) {
        return id != null && ObjectId.isValid(id) ? Optional.of(new ObjectId(id)) : Optional.empty();
    }

    /** Un id que ya se validó (la oferta existe en el catálogo, o lo generó el adaptador), así que tiene ese formato. */
    private static ObjectId referencia(String id) {
        return objectId(id).orElseThrow(() ->
                new IllegalArgumentException("El id %s no tiene formato ObjectId (DC-30)".formatted(id)));
    }
}
