package co.edu.unimagdalena.samariantour.catalogo.opiniones.infraestructura.salida.persistencia.document;

import co.edu.unimagdalena.samariantour.catalogo.opiniones.dominio.modelo.EstadoOpinion;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.UUID;

/**
 * Una opinión de la colección {@code opiniones} (DC-46). {@code ofertaId} es una referencia {@code ObjectId} a la
 * oferta (DC-30) y {@code turistaId} el UUID que emite el núcleo.
 *
 * @param motivoRechazo solo si fue rechazada
 * @param revisadaEn    solo si ya se moderó
 * @param revision      revisión del bloqueo optimista (ADR-05); el adaptador la compara y la sube al guardar
 */
@Document(OpinionDocument.COLECCION)
public record OpinionDocument(
        @Id ObjectId id,
        ObjectId ofertaId,
        UUID turistaId,
        int calificacion,
        String comentario,
        Instant fecha,
        EstadoOpinion estado,
        String motivoRechazo,
        Instant revisadaEn,
        long revision) {

    public static final String COLECCION = "opiniones";

    public static final String OFERTA_ID = "ofertaId";
    public static final String TURISTA_ID = "turistaId";
    public static final String CALIFICACION = "calificacion";
    public static final String FECHA = "fecha";
    public static final String ESTADO = "estado";
    public static final String REVISION = "revision";
}
