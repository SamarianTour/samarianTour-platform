package co.edu.unimagdalena.samariantour.catalogo.catalogo.infraestructura.salida.almacenamiento;

import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.excepcion.DatoInvalidoException;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.TamanoFoto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import static co.edu.unimagdalena.samariantour.catalogo.catalogo.infraestructura.salida.almacenamiento.ImagenesDePrueba.leer;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProcesadorImagenesTest {

    private final ProcesadorImagenes procesador = new ProcesadorImagenes();

    @Test
    @DisplayName("reduce cada tamaño hasta su lado máximo, sin deformar la imagen")
    void reduceSinDeformar() {
        Map<TamanoFoto, byte[]> tamanos = procesador.procesar(ImagenesDePrueba.jpg(3000, 2000));

        assertThat(leer(tamanos.get(TamanoFoto.GALERIA)))
                .satisfies(imagen -> assertThat(imagen.getWidth()).isEqualTo(1280))
                .satisfies(imagen -> assertThat(imagen.getHeight()).isBetween(852, 854));
        assertThat(leer(tamanos.get(TamanoFoto.MINIATURA)))
                .satisfies(imagen -> assertThat(imagen.getWidth()).isEqualTo(400))
                .satisfies(imagen -> assertThat(imagen.getHeight()).isBetween(266, 268));
    }

    @Test
    @DisplayName("una imagen más pequeña que el tamaño no se agranda")
    void noAgranda() {
        Map<TamanoFoto, byte[]> tamanos = procesador.procesar(ImagenesDePrueba.jpg(300, 200));

        for (byte[] tamano : tamanos.values()) {
            assertThat(leer(tamano).getWidth()).isEqualTo(300);
        }
    }

    @Test
    @DisplayName("endereza la foto según su orientación EXIF y la guarda sin metadatos")
    void enderezaYQuitaMetadatos() {
        // Orientación 6: la cámara la guardó acostada y hay que girarla 90° a la derecha para verla bien.
        byte[] original = ImagenesDePrueba.jpgConOrientacion(200, 100, 6);
        assertThat(new String(original, StandardCharsets.ISO_8859_1)).contains("Exif");

        byte[] guardada = procesador.procesar(original).get(TamanoFoto.GALERIA);

        BufferedImage derecha = leer(guardada);
        assertThat(derecha.getWidth()).isEqualTo(100);
        assertThat(derecha.getHeight()).isEqualTo(200);
        // Girada a la derecha, la mitad izquierda (roja) queda arriba y la derecha (azul) abajo.
        assertThat(rojoDe(derecha, 50, 30)).isGreaterThan(200);
        assertThat(azulDe(derecha, 50, 170)).isGreaterThan(200);
        assertThat(new String(guardada, StandardCharsets.ISO_8859_1)).doesNotContain("Exif");
    }

    @Test
    @DisplayName("acepta PNG y pone la transparencia sobre fondo blanco, porque JPG no tiene transparencia")
    void pngTransparente() {
        BufferedImage guardada = leer(procesador.procesar(ImagenesDePrueba.pngTransparente(40, 40))
                .get(TamanoFoto.MINIATURA));

        assertThat(new Color(guardada.getRGB(20, 20))).satisfies(color -> {
            assertThat(color.getRed()).isGreaterThan(245);
            assertThat(color.getGreen()).isGreaterThan(245);
            assertThat(color.getBlue()).isGreaterThan(245);
        });
    }

    @Test
    @DisplayName("acepta WebP y lo guarda como JPG")
    void webp() {
        Map<TamanoFoto, byte[]> tamanos = procesador.procesar(ImagenesDePrueba.webp());

        assertThat(leer(tamanos.get(TamanoFoto.GALERIA)).getWidth()).isEqualTo(1);
        assertThat(tamanos.get(TamanoFoto.GALERIA)).startsWith((byte) 0xFF, (byte) 0xD8);
    }

    @Test
    @DisplayName("reconoce el formato por el contenido: rechaza lo que no es imagen y los formatos no aceptados")
    void formatosNoAceptados() {
        assertThatThrownBy(() -> procesador.procesar("<script>alert(1)</script>".getBytes(StandardCharsets.UTF_8)))
                .isInstanceOf(DatoInvalidoException.class)
                .hasMessageContaining("JPG, PNG o WebP");
        assertThatThrownBy(() -> procesador.procesar(ImagenesDePrueba.gif(10, 10)))
                .isInstanceOf(DatoInvalidoException.class)
                .hasMessageContaining("JPG, PNG o WebP");
    }

    @Test
    @DisplayName("rechaza una imagen dañada y una que dice tener demasiados píxeles, sin intentar decodificarla")
    void imagenesPeligrosas() {
        byte[] jpg = ImagenesDePrueba.jpg(100, 100);
        byte[] cortada = java.util.Arrays.copyOf(jpg, 40);

        assertThatThrownBy(() -> procesador.procesar(cortada)).isInstanceOf(DatoInvalidoException.class);
        assertThatThrownBy(() -> procesador.procesar(ImagenesDePrueba.cabeceraPng(20_000, 20_000)))
                .isInstanceOf(DatoInvalidoException.class)
                .hasMessageContaining("píxeles");
    }

    private static int rojoDe(BufferedImage imagen, int x, int y) {
        return new Color(imagen.getRGB(x, y)).getRed();
    }

    private static int azulDe(BufferedImage imagen, int x, int y) {
        return new Color(imagen.getRGB(x, y)).getBlue();
    }
}
