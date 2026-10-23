package co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo;

import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.excepcion.DatoInvalidoException;

import java.util.Set;
import java.util.UUID;

/**
 * Una foto guardada en el almacenamiento de imágenes (RF-64, DC-39). El catálogo guarda solo su identificador; la API
 * arma la dirección de cada tamaño, y el archivo vive en el almacenamiento, detrás de FileStoragePort.
 *
 * @param id identificador aleatorio que asigna el almacenamiento; nunca el nombre del archivo que se subió
 */
public record Foto(String id) {

    /** Tamaño máximo del archivo que se sube, antes de reprocesarlo: 5 MB. */
    public static final int TAMANO_MAXIMO_BYTES = 5 * 1024 * 1024;

    /** Formatos que se aceptan al subir; el almacenamiento los reconoce por su contenido, no por la extensión. */
    public static final Set<String> FORMATOS_ACEPTADOS = Set.of("JPG", "PNG", "WebP");

    public Foto {
        Validaciones.texto(id, "fotoId");
        try {
            id = UUID.fromString(id).toString();
        } catch (IllegalArgumentException e) {
            throw new DatoInvalidoException("fotoId", "no es un identificador de foto");
        }
    }

    /** Una foto nueva, con un identificador que no se puede adivinar. */
    public static Foto nueva() {
        return new Foto(UUID.randomUUID().toString());
    }
}
