package co.edu.unimagdalena.samariantour.catalogo.opiniones.infraestructura.salida.persistencia.document;

import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.FieldType;
import org.springframework.data.mongodb.core.mapping.MongoId;

/**
 * Un aviso de opiniones ya aplicado, colección {@code avisos_opiniones_procesados} (DC-46). El {@code _id} es el
 * idAviso, así que la base garantiza que no se repita entre réplicas (ADR-05).
 */
@Document(AvisoOpinionProcesadoDocument.COLECCION)
public record AvisoOpinionProcesadoDocument(
        // Siempre texto: sin esto, Spring guardaría como ObjectId un idAviso que tenga ese formato.
        @MongoId(FieldType.STRING) String id) {

    public static final String COLECCION = "avisos_opiniones_procesados";
}
