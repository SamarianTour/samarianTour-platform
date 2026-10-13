package co.edu.unimagdalena.samariantour.catalogo.catalogo.infraestructura.salida.persistencia.document;

import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.TipoOferta;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.UUID;

/**
 * Una oferta de la colección {@code ofertas} (DC-16): la identidad, la copia de la habilitación RNT y las dos
 * versiones. {@code publicada} no existe hasta la primera aprobación (DC-06) y {@code pendiente} no existe si no hay
 * una versión en moderación o rechazada.
 *
 * @param revision revisión del bloqueo optimista (ADR-05); el adaptador la compara y la sube al guardar
 */
@Document(OfertaDocument.COLECCION)
public record OfertaDocument(
        @Id ObjectId id,
        TipoOferta tipo,
        UUID prestadorId,
        boolean retirada,
        boolean rntPrestadorActivo,
        long versionHabilitacion,
        long revision,
        VersionDocument publicada,
        VersionDocument pendiente) {

    public static final String COLECCION = "ofertas";

    public static final String TIPO = "tipo";
    public static final String PRESTADOR_ID = "prestadorId";
    public static final String RETIRADA = "retirada";
    public static final String RNT_PRESTADOR_ACTIVO = "rntPrestadorActivo";
    public static final String VERSION_HABILITACION = "versionHabilitacion";
    public static final String REVISION = "revision";
    public static final String PUBLICADA = "publicada";
}
