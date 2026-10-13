package co.edu.unimagdalena.samariantour.catalogo.catalogo.infraestructura.salida.persistencia.document;

import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;
import java.util.UUID;

/**
 * El perfil público de un prestador, colección {@code perfiles} (RF-24, DC-26). Hay a lo sumo uno por prestador:
 * {@code prestadorId} tiene un índice único.
 */
@Document(PerfilDocument.COLECCION)
public record PerfilDocument(
        @Id ObjectId id,
        UUID prestadorId,
        String descripcion,
        List<String> fotos,
        String horariosAtencion,
        String sitioWeb,
        List<String> redesSociales) {

    public static final String COLECCION = "perfiles";

    public static final String PRESTADOR_ID = "prestadorId";
}
