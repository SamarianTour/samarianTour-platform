package co.edu.unimagdalena.samariantour.catalogo.catalogo.infraestructura.salida.almacenamiento;

import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.excepcion.DatoInvalidoException;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.TamanoFoto;
import com.drew.imaging.ImageMetadataReader;
import com.drew.imaging.ImageProcessingException;
import com.drew.metadata.Metadata;
import com.drew.metadata.MetadataException;
import com.drew.metadata.exif.ExifIFD0Directory;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageReadParam;
import javax.imageio.ImageReader;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.ImageOutputStream;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.EnumMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Convierte la imagen que subió el usuario en las que se guardan (DC-39). Lo usan los dos adaptadores de
 * FileStoragePort, así que guardan exactamente lo mismo.
 *
 * <ol>
 *   <li>Reconoce el formato por el contenido del archivo, no por su nombre ni por el tipo que declara el navegador.
 *       Solo acepta JPG, PNG y WebP.</li>
 *   <li>Decodifica la imagen ya reducida (submuestreo), a no más del doble del lado de la galería: la memoria que
 *       ocupa no depende de los megapíxeles de la cámara. El catálogo corre con 512 MB.</li>
 *   <li>La endereza según su orientación EXIF: los celulares guardan la foto acostada y anotan cómo girarla.</li>
 *   <li>Genera cada {@link TamanoFoto} como un JPG nuevo, sobre fondo blanco si tenía transparencia. Las imágenes
 *       nuevas no llevan los metadatos del original, por ejemplo la ubicación GPS.</li>
 * </ol>
 */
final class ProcesadorImagenes {

    /** Ninguna foto razonable pasa de 100 megapíxeles; más que eso es un archivo hecho para agotar la memoria. */
    static final long PIXELES_MAXIMOS = 100_000_000L;

    private static final Set<String> FORMATOS = Set.of("jpeg", "jpg", "png", "webp");
    private static final float CALIDAD_JPG = 0.85f;
    private static final int LADO_DECODIFICADO = 2 * TamanoFoto.GALERIA.ladoMaximo();

    static {
        // En el jar de Spring Boot, ImageIO no descubre solo los lectores de las dependencias, como el de WebP.
        ImageIO.scanForPlugins();
    }

    /** Las imágenes que se guardan, una por tamaño, como JPG. */
    Map<TamanoFoto, byte[]> procesar(byte[] imagen) {
        BufferedImage derecha = enderezar(decodificar(imagen), orientacion(imagen));
        Map<TamanoFoto, byte[]> resultado = new EnumMap<>(TamanoFoto.class);
        for (TamanoFoto tamano : TamanoFoto.values()) {
            resultado.put(tamano, aJpg(reducir(derecha, tamano.ladoMaximo())));
        }
        return resultado;
    }

    private static BufferedImage decodificar(byte[] imagen) {
        try (ImageInputStream entrada = ImageIO.createImageInputStream(new ByteArrayInputStream(imagen))) {
            Iterator<ImageReader> lectores = entrada == null ? null : ImageIO.getImageReaders(entrada);
            if (lectores == null || !lectores.hasNext()) {
                throw formatoNoAceptado();
            }
            ImageReader lector = lectores.next();
            try {
                if (!FORMATOS.contains(lector.getFormatName().toLowerCase(Locale.ROOT))) {
                    throw formatoNoAceptado();
                }
                lector.setInput(entrada, true, true);
                int ancho = lector.getWidth(0);
                int alto = lector.getHeight(0);
                if ((long) ancho * alto > PIXELES_MAXIMOS) {
                    throw new DatoInvalidoException("foto", "tiene demasiados píxeles para ser una foto");
                }
                ImageReadParam parametros = lector.getDefaultReadParam();
                int paso = Math.max(1, (int) Math.ceil((double) Math.max(ancho, alto) / LADO_DECODIFICADO));
                parametros.setSourceSubsampling(paso, paso, 0, 0);
                return lector.read(0, parametros);
            } finally {
                lector.dispose();
            }
        } catch (IOException | RuntimeException e) {
            if (e instanceof DatoInvalidoException invalida) {
                throw invalida;
            }
            throw new DatoInvalidoException("foto", "no se pudo leer la imagen; puede estar dañada");
        }
    }

    /** La orientación EXIF (1 a 8); 1, la normal, si la imagen no la trae. */
    private static int orientacion(byte[] imagen) {
        try {
            Metadata metadatos = ImageMetadataReader.readMetadata(new ByteArrayInputStream(imagen));
            ExifIFD0Directory exif = metadatos.getFirstDirectoryOfType(ExifIFD0Directory.class);
            if (exif != null && exif.containsTag(ExifIFD0Directory.TAG_ORIENTATION)) {
                return exif.getInt(ExifIFD0Directory.TAG_ORIENTATION);
            }
        } catch (ImageProcessingException | IOException | MetadataException e) {
            // Sin metadatos legibles la imagen se deja como viene.
        }
        return 1;
    }

    /**
     * Aplica la orientación EXIF y aplana la transparencia sobre blanco, en un solo dibujo. Cada transformación lleva
     * el punto (x, y) de la imagen original a su lugar en la imagen derecha.
     */
    static BufferedImage enderezar(BufferedImage original, int orientacion) {
        int ancho = original.getWidth();
        int alto = original.getHeight();
        AffineTransform giro = switch (orientacion) {
            case 2 -> new AffineTransform(-1, 0, 0, 1, ancho, 0);
            case 3 -> new AffineTransform(-1, 0, 0, -1, ancho, alto);
            case 4 -> new AffineTransform(1, 0, 0, -1, 0, alto);
            case 5 -> new AffineTransform(0, 1, 1, 0, 0, 0);
            case 6 -> new AffineTransform(0, 1, -1, 0, alto, 0);
            case 7 -> new AffineTransform(0, -1, -1, 0, alto, ancho);
            case 8 -> new AffineTransform(0, -1, 1, 0, 0, ancho);
            default -> new AffineTransform();
        };
        boolean acostada = orientacion >= 5 && orientacion <= 8;
        BufferedImage derecha = new BufferedImage(acostada ? alto : ancho, acostada ? ancho : alto,
                BufferedImage.TYPE_INT_RGB);
        Graphics2D lienzo = derecha.createGraphics();
        try {
            lienzo.setColor(Color.WHITE);
            lienzo.fillRect(0, 0, derecha.getWidth(), derecha.getHeight());
            lienzo.drawImage(original, giro, null);
        } finally {
            lienzo.dispose();
        }
        return derecha;
    }

    /** Reduce hasta que el lado más largo no pase de {@code ladoMaximo}, a la mitad por paso para no perder detalle. */
    static BufferedImage reducir(BufferedImage imagen, int ladoMaximo) {
        BufferedImage actual = imagen;
        while (Math.max(actual.getWidth(), actual.getHeight()) > ladoMaximo) {
            double escala = Math.max(0.5, (double) ladoMaximo / Math.max(actual.getWidth(), actual.getHeight()));
            int ancho = Math.max(1, (int) Math.round(actual.getWidth() * escala));
            int alto = Math.max(1, (int) Math.round(actual.getHeight() * escala));
            BufferedImage menor = new BufferedImage(ancho, alto, BufferedImage.TYPE_INT_RGB);
            Graphics2D lienzo = menor.createGraphics();
            try {
                lienzo.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                lienzo.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
                lienzo.drawImage(actual, 0, 0, ancho, alto, null);
            } finally {
                lienzo.dispose();
            }
            actual = menor;
        }
        return actual;
    }

    private static byte[] aJpg(BufferedImage imagen) {
        ImageWriter escritor = ImageIO.getImageWritersByFormatName("jpeg").next();
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ImageOutputStream salida = ImageIO.createImageOutputStream(bytes)) {
            escritor.setOutput(salida);
            ImageWriteParam parametros = escritor.getDefaultWriteParam();
            parametros.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
            parametros.setCompressionQuality(CALIDAD_JPG);
            escritor.write(null, new IIOImage(imagen, null, null), parametros);
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo generar el JPG", e);
        } finally {
            escritor.dispose();
        }
        return bytes.toByteArray();
    }

    private static DatoInvalidoException formatoNoAceptado() {
        return new DatoInvalidoException("foto", "debe ser una imagen JPG, PNG o WebP");
    }
}
