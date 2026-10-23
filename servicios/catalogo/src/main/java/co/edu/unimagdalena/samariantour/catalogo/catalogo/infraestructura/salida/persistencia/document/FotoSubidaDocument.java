package co.edu.unimagdalena.samariantour.catalogo.catalogo.infraestructura.salida.persistencia.document;

import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.DestinoFoto;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.UUID;

/**
 * Una foto subida, colección {@code fotos} (DC-47). El archivo vive en el almacenamiento de imágenes; aquí solo queda
 * de quién es, para qué se subió y cuándo se usó por última vez.
 *
 * @param id identificador de la foto (UUID en texto), el mismo que usan las ofertas y los perfiles
 */
@Document(FotoSubidaDocument.COLECCION)
public record FotoSubidaDocument(
        @Id String id,
        UUID prestadorId,
        DestinoFoto destino,
        Instant ultimoUso) {

    public static final String COLECCION = "fotos";
    public static final String ULTIMO_USO = "ultimoUso";
}
