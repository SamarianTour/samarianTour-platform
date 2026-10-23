package co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.puerto.salida;

import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.Foto;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.TamanoFoto;

import java.util.Optional;

/**
 * Almacenamiento de las fotos (RF-64, DC-39). En el piloto lo implementa MinIO; sin él, una carpeta local.
 *
 * <p>Nunca se guarda el archivo que subió el usuario: el almacenamiento reconoce la imagen por su contenido, la
 * endereza según su orientación EXIF y guarda, para cada {@link TamanoFoto}, una imagen JPG nueva sin metadatos (por
 * ejemplo, sin la ubicación GPS con que la tomó un celular).
 */
public interface FileStoragePort {

    /**
     * Guarda la imagen como esa foto, en todos sus tamaños.
     *
     * @throws co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.excepcion.DatoInvalidoException si no es una imagen de un formato aceptado ({@link Foto#FORMATOS_ACEPTADOS}) o no se puede leer
     */
    void guardar(Foto foto, byte[] imagen);

    /** La foto en ese tamaño, como JPG; vacío si no existe. */
    Optional<byte[]> leer(Foto foto, TamanoFoto tamano);

    /** Borra la foto en todos sus tamaños; si no existe, no hace nada. */
    void eliminar(Foto foto);
}
