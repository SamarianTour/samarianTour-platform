package co.edu.unimagdalena.samariantour.catalogo.opiniones.infraestructura.salida.persistencia.document;

import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.UUID;

/**
 * Una habilitación para opinar, colección {@code habilitaciones_opinion} (DC-41). El índice único
 * {@code {turistaId, ofertaId}} garantiza una por turista y oferta, también entre réplicas.
 */
@Document(HabilitacionOpinionDocument.COLECCION)
public record HabilitacionOpinionDocument(
        @Id ObjectId id,
        UUID turistaId,
        ObjectId ofertaId) {

    public static final String COLECCION = "habilitaciones_opinion";

    public static final String TURISTA_ID = "turistaId";
    public static final String OFERTA_ID = "ofertaId";
}
