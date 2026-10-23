package co.edu.unimagdalena.samariantour.catalogo.catalogo.infraestructura.salida.demo;

import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.Foto;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.TamanoFoto;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.puerto.salida.FileStoragePort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.Map;

/**
 * Guarda en el almacenamiento las fotos de las ofertas de demostración al arrancar, con el perfil {@code demo}
 * (DC-47). Solo guarda las que faltan, así que no repite el trabajo en cada arranque; si dos réplicas guardan la misma
 * a la vez, la guardan igual.
 *
 * <p>Las imágenes están en {@code src/main/resources/demo/fotos} ({@link DatosSantaMarta#archivosDeFotos()}). Una que
 * falte no impide arrancar: la oferta queda con una foto que responde 404.
 */
@Component
@Profile("demo")
public class CargaFotosDemo implements ApplicationRunner {

    private static final Logger LOG = LoggerFactory.getLogger(CargaFotosDemo.class);

    private final FileStoragePort almacenamiento;

    public CargaFotosDemo(FileStoragePort almacenamiento) {
        this.almacenamiento = almacenamiento;
    }

    @Override
    public void run(ApplicationArguments argumentos) {
        cargar();
    }

    /** @return cuántas fotos guardó */
    public int cargar() {
        int guardadas = 0;
        for (Map.Entry<Foto, String> foto : DatosSantaMarta.archivosDeFotos().entrySet()) {
            if (almacenamiento.leer(foto.getKey(), TamanoFoto.MINIATURA).isPresent()) {
                continue;
            }
            ClassPathResource archivo = new ClassPathResource(foto.getValue());
            if (!archivo.exists()) {
                LOG.warn("Falta la foto de demostración {}", foto.getValue());
                continue;
            }
            almacenamiento.guardar(foto.getKey(), leer(archivo));
            guardadas++;
        }
        LOG.info("Fotos de demostración guardadas por esta réplica: {}", guardadas);
        return guardadas;
    }

    private static byte[] leer(ClassPathResource archivo) {
        try (InputStream entrada = archivo.getInputStream()) {
            return entrada.readAllBytes();
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo leer " + archivo.getPath(), e);
        }
    }
}
