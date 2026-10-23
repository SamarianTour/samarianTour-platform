package co.edu.unimagdalena.samariantour.catalogo.catalogo.infraestructura.salida.almacenamiento;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.zip.CRC32;

/**
 * Imágenes que se generan en las pruebas, para no versionar archivos binarios.
 */
public final class ImagenesDePrueba {

    /** Un WebP con pérdida de 1 × 1 píxeles, el mismo con que los navegadores prueban si leen WebP. */
    private static final String WEBP_1X1 = "UklGRiIAAABXRUJQVlA4IBYAAAAwAQCdASoBAAEADsD+JaQAA3AAAAAA";

    private ImagenesDePrueba() {
    }

    /** Un JPG con la mitad izquierda roja y la derecha azul, para ver hacia dónde quedó girada. */
    public static byte[] jpg(int ancho, int alto) {
        return codificar(dosColores(ancho, alto, BufferedImage.TYPE_INT_RGB), "jpeg");
    }

    /** Un PNG totalmente transparente. */
    public static byte[] pngTransparente(int ancho, int alto) {
        return codificar(new BufferedImage(ancho, alto, BufferedImage.TYPE_INT_ARGB), "png");
    }

    public static byte[] gif(int ancho, int alto) {
        return codificar(dosColores(ancho, alto, BufferedImage.TYPE_INT_RGB), "gif");
    }

    public static byte[] webp() {
        return Base64.getDecoder().decode(WEBP_1X1);
    }

    /**
     * El JPG de {@link #jpg} con un bloque EXIF que trae su orientación, como lo guarda un celular. El bloque va justo
     * después del marcador de inicio de la imagen.
     */
    public static byte[] jpgConOrientacion(int ancho, int alto, int orientacion) {
        byte[] jpg = jpg(ancho, alto);
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (DataOutputStream salida = new DataOutputStream(bytes)) {
            salida.write(jpg, 0, 2);
            salida.writeShort(0xFFE1);
            salida.writeShort(34);
            salida.write("Exif".getBytes(StandardCharsets.US_ASCII));
            salida.writeShort(0);
            salida.write(new byte[] {'M', 'M', 0, 0x2A, 0, 0, 0, 8});
            salida.writeShort(1);
            salida.writeShort(0x0112);
            salida.writeShort(3);
            salida.writeInt(1);
            salida.writeShort(orientacion);
            salida.writeShort(0);
            salida.writeInt(0);
            salida.write(jpg, 2, jpg.length - 2);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return bytes.toByteArray();
    }

    /** Un PNG que dice medir {@code ancho} × {@code alto} pero no trae píxeles: solo la cabecera. */
    public static byte[] cabeceraPng(int ancho, int alto) {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (DataOutputStream salida = new DataOutputStream(bytes)) {
            salida.write(new byte[] {(byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1A, '\n'});
            ByteArrayOutputStream ihdr = new ByteArrayOutputStream();
            try (DataOutputStream datos = new DataOutputStream(ihdr)) {
                datos.write("IHDR".getBytes(StandardCharsets.US_ASCII));
                datos.writeInt(ancho);
                datos.writeInt(alto);
                datos.write(new byte[] {8, 2, 0, 0, 0});
            }
            byte[] bloque = ihdr.toByteArray();
            CRC32 crc = new CRC32();
            crc.update(bloque);
            salida.writeInt(bloque.length - 4);
            salida.write(bloque);
            salida.writeInt((int) crc.getValue());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return bytes.toByteArray();
    }

    public static BufferedImage leer(byte[] imagen) {
        try {
            return ImageIO.read(new ByteArrayInputStream(imagen));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static BufferedImage dosColores(int ancho, int alto, int tipo) {
        BufferedImage imagen = new BufferedImage(ancho, alto, tipo);
        Graphics2D lienzo = imagen.createGraphics();
        lienzo.setColor(Color.RED);
        lienzo.fillRect(0, 0, ancho / 2, alto);
        lienzo.setColor(Color.BLUE);
        lienzo.fillRect(ancho / 2, 0, ancho - ancho / 2, alto);
        lienzo.dispose();
        return imagen;
    }

    private static byte[] codificar(BufferedImage imagen, String formato) {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try {
            ImageIO.write(imagen, formato, bytes);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return bytes.toByteArray();
    }
}
