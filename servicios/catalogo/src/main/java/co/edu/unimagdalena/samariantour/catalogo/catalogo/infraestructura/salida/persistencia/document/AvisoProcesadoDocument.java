package co.edu.unimagdalena.samariantour.catalogo.catalogo.infraestructura.salida.persistencia.document;

import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.FieldType;
import org.springframework.data.mongodb.core.mapping.MongoId;

/**
 * Un aviso del núcleo ya aplicado, colección {@code avisos_procesados} (DC-04). El {@code _id} es el idAviso, así que
 * la base garantiza que no se repita entre réplicas (ADR-05).
 */
@Document(AvisoProcesadoDocument.COLECCION)
public record AvisoProcesadoDocument(
        // Siempre texto: sin esto, Spring guardaría como ObjectId un idAviso que tenga ese formato.
        @MongoId(FieldType.STRING) String id) {

    public static final String COLECCION = "avisos_procesados";
}
