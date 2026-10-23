package co.edu.unimagdalena.samariantour.catalogo.catalogo.infraestructura.salida.almacenamiento;

import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.excepcion.DatoInvalidoException;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.Foto;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.TamanoFoto;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.puerto.salida.FileStoragePort;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static co.edu.unimagdalena.samariantour.catalogo.catalogo.infraestructura.salida.almacenamiento.ImagenesDePrueba.leer;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Contrato de FileStoragePort, común a la carpeta local y a MinIO (DC-39).
 */
interface ContratoFileStorage {

    FileStoragePort fotos();

    @Test
    @DisplayName("guarda la foto en cada tamaño como JPG y la entrega")
    default void guardaYEntrega() {
        Foto foto = Foto.nueva();

        fotos().guardar(foto, ImagenesDePrueba.jpg(2000, 1000));

        assertThat(fotos().leer(foto, TamanoFoto.GALERIA)).hasValueSatisfying(jpg ->
                assertThat(leer(jpg).getWidth()).isEqualTo(1280));
        assertThat(fotos().leer(foto, TamanoFoto.MINIATURA)).hasValueSatisfying(jpg ->
                assertThat(leer(jpg).getWidth()).isEqualTo(400));
    }

    @Test
    @DisplayName("una foto que no existe no se encuentra, y borrarla no es un error")
    default void fotoInexistente() {
        Foto foto = Foto.nueva();

        assertThat(fotos().leer(foto, TamanoFoto.GALERIA)).isEmpty();
        fotos().eliminar(foto);
    }

    @Test
    @DisplayName("borrar la foto la quita en todos sus tamaños; borrarla otra vez no es un error")
    default void eliminar() {
        Foto foto = Foto.nueva();
        fotos().guardar(foto, ImagenesDePrueba.jpg(100, 100));

        fotos().eliminar(foto);
        fotos().eliminar(foto);

        for (TamanoFoto tamano : TamanoFoto.values()) {
            assertThat(fotos().leer(foto, tamano)).isEmpty();
        }
    }

    @Test
    @DisplayName("lo que no es una imagen aceptada no se guarda")
    default void imagenInvalida() {
        Foto foto = Foto.nueva();

        assertThatThrownBy(() -> fotos().guardar(foto, "no soy una foto".getBytes(StandardCharsets.UTF_8)))
                .isInstanceOf(DatoInvalidoException.class);
        assertThat(fotos().leer(foto, TamanoFoto.GALERIA)).isEmpty();
    }
}
