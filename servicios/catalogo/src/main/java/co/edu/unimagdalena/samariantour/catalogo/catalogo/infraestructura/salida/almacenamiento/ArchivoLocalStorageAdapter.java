package co.edu.unimagdalena.samariantour.catalogo.catalogo.infraestructura.salida.almacenamiento;

import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.Foto;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.TamanoFoto;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.puerto.salida.FileStoragePort;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Comparator;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Las fotos en una carpeta del equipo, para correr el catálogo sin MinIO (DC-39). Solo sirve con una réplica: con
 * varias, cada una tendría su carpeta. En Docker Compose se usa MinIO.
 *
 * <p>Cada foto es una carpeta con un JPG por tamaño: {@code <carpeta>/<id>/galeria.jpg} y {@code miniatura.jpg}.
 */
@Component
@Profile("!minio")
public class ArchivoLocalStorageAdapter implements FileStoragePort {

    private final Path carpeta;
    private final ProcesadorImagenes procesador = new ProcesadorImagenes();

    public ArchivoLocalStorageAdapter(
            @Value("${catalogo.fotos.carpeta:${java.io.tmpdir}/samarian-tour/fotos}") Path carpeta) {
        this.carpeta = carpeta;
    }

    @Override
    public void guardar(Foto foto, byte[] imagen) {
        Map<TamanoFoto, byte[]> tamanos = procesador.procesar(imagen);
        try {
            Files.createDirectories(carpetaDe(foto));
            for (Map.Entry<TamanoFoto, byte[]> tamano : tamanos.entrySet()) {
                // Se escribe aparte y se mueve: quien lea en ese momento ve la foto completa o no la ve.
                Path temporal = Files.createTempFile(carpetaDe(foto), "subida", ".tmp");
                Files.write(temporal, tamano.getValue());
                Files.move(temporal, archivo(foto, tamano.getKey()), StandardCopyOption.REPLACE_EXISTING,
                        StandardCopyOption.ATOMIC_MOVE);
            }
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo guardar la foto " + foto.id(), e);
        }
    }

    @Override
    public Optional<byte[]> leer(Foto foto, TamanoFoto tamano) {
        try {
            return Optional.of(Files.readAllBytes(archivo(foto, tamano)));
        } catch (NoSuchFileException e) {
            return Optional.empty();
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo leer la foto " + foto.id(), e);
        }
    }

    @Override
    public void eliminar(Foto foto) {
        if (!Files.exists(carpetaDe(foto))) {
            return;
        }
        try (Stream<Path> archivos = Files.walk(carpetaDe(foto))) {
            for (Path archivo : archivos.sorted(Comparator.reverseOrder()).toList()) {
                Files.deleteIfExists(archivo);
            }
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo borrar la foto " + foto.id(), e);
        }
    }

    /** El id de la foto es un UUID validado por el dominio, así que no puede salirse de la carpeta. */
    private Path carpetaDe(Foto foto) {
        return carpeta.resolve(foto.id());
    }

    private Path archivo(Foto foto, TamanoFoto tamano) {
        return carpetaDe(foto).resolve(tamano.name().toLowerCase(Locale.ROOT) + ".jpg");
    }
}
