package co.edu.unimagdalena.samariantour.catalogo.catalogo.infraestructura.salida.persistencia.document;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.UUID;

/**
 * La réplica de la habilitación RNT de un prestador, colección {@code prestadores} (DC-02). El id es el UUID que
 * emite el núcleo.
 */
@Document(PrestadorDocument.COLECCION)
public record PrestadorDocument(
        @Id UUID id,
        boolean rntActivo,
        long version) {

    public static final String COLECCION = "prestadores";

    public static final String RNT_ACTIVO = "rntActivo";
    public static final String VERSION = "version";
}
