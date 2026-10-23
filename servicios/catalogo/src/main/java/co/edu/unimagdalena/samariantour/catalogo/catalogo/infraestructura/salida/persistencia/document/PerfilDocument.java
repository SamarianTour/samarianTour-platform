package co.edu.unimagdalena.samariantour.catalogo.catalogo.infraestructura.salida.persistencia.document;

import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;
import java.util.UUID;

/**
 * El perfil público de un prestador, colección {@code perfiles} (RF-24, RF-64, DC-26). Hay a lo sumo uno por
 * prestador: {@code prestadorId} tiene un índice único.
 *
 * @param fotos    identificadores de las fotos en el almacenamiento (DC-39); no URLs
 * @param revision revisión para el bloqueo optimista (ADR-05)
 */
@Document(PerfilDocument.COLECCION)
public record PerfilDocument(
        @Id ObjectId id,
        UUID prestadorId,
        String nombreComercial,
        String descripcion,
        List<String> fotos,
        String horariosAtencion,
        String sitioWeb,
        List<String> redesSociales,
        long revision) {

    public static final String COLECCION = "perfiles";
    public static final String PRESTADOR_ID = "prestadorId";
    public static final String NOMBRE_COMERCIAL = "nombreComercial";
    public static final String DESCRIPCION = "descripcion";
    public static final String FOTOS = "fotos";
    public static final String HORARIOS_ATENCION = "horariosAtencion";
    public static final String SITIO_WEB = "sitioWeb";
    public static final String REDES_SOCIALES = "redesSociales";
    public static final String REVISION = "revision";
}
