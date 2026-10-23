package co.edu.unimagdalena.samariantour.catalogo.catalogo.infraestructura.salida.almacenamiento;

import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.Foto;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.TamanoFoto;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.puerto.salida.FileStoragePort;
import io.minio.BucketExistsArgs;
import io.minio.GetObjectArgs;
import io.minio.GetObjectResponse;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import io.minio.errors.ErrorResponseException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * Las fotos en MinIO, el almacenamiento de objetos del piloto (DC-39). Las réplicas del catálogo comparten el mismo
 * bucket, que no se expone hacia afuera: el navegador pide las fotos al catálogo.
 *
 * <p>Cada tamaño es un objeto: {@code fotos/<id>/galeria.jpg} y {@code fotos/<id>/miniatura.jpg}.
 */
@Component
@Profile("minio")
public class MinioStorageAdapter implements FileStoragePort, InitializingBean {

    private static final String PREFIJO = "fotos/";
    private static final String CONTENT_TYPE_JPG = "image/jpeg";

    private final MinioClient minio;
    private final String bucket;
    private final ProcesadorImagenes procesador = new ProcesadorImagenes();

    public MinioStorageAdapter(@Value("${catalogo.minio.url}") String url,
                               @Value("${catalogo.minio.usuario}") String usuario,
                               @Value("${catalogo.minio.clave}") String clave,
                               @Value("${catalogo.minio.bucket:catalogo}") String bucket) {
        this.minio = MinioClient.builder().endpoint(url).credentials(usuario, clave).build();
        this.bucket = bucket;
    }

    /** Crea el bucket al arrancar si no existe. Las dos réplicas pueden intentarlo a la vez. */
    @Override
    public void afterPropertiesSet() {
        try {
            if (!minio.bucketExists(BucketExistsArgs.builder().bucket(bucket).build())) {
                minio.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());
            }
        } catch (ErrorResponseException e) {
            if (!"BucketAlreadyOwnedByYou".equals(e.errorResponse().code())) {
                throw new IllegalStateException("No se pudo crear el bucket " + bucket + " en MinIO", e);
            }
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo conectar con MinIO para preparar el bucket " + bucket, e);
        }
    }

    @Override
    public void guardar(Foto foto, byte[] imagen) {
        for (Map.Entry<TamanoFoto, byte[]> tamano : procesador.procesar(imagen).entrySet()) {
            byte[] contenido = tamano.getValue();
            try {
                minio.putObject(PutObjectArgs.builder()
                        .bucket(bucket)
                        .object(objeto(foto, tamano.getKey()))
                        .stream(new ByteArrayInputStream(contenido), contenido.length, -1)
                        .contentType(CONTENT_TYPE_JPG)
                        .build());
            } catch (Exception e) {
                throw new IllegalStateException("No se pudo guardar la foto " + foto.id() + " en MinIO", e);
            }
        }
    }

    @Override
    public Optional<byte[]> leer(Foto foto, TamanoFoto tamano) {
        try (GetObjectResponse objeto = minio.getObject(
                GetObjectArgs.builder().bucket(bucket).object(objeto(foto, tamano)).build())) {
            return Optional.of(objeto.readAllBytes());
        } catch (ErrorResponseException e) {
            if ("NoSuchKey".equals(e.errorResponse().code())) {
                return Optional.empty();
            }
            throw new IllegalStateException("No se pudo leer la foto " + foto.id() + " de MinIO", e);
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo leer la foto " + foto.id() + " de MinIO", e);
        }
    }

    /** Borrar un objeto que no existe no es un error en MinIO, así que borrar dos veces tampoco lo es. */
    @Override
    public void eliminar(Foto foto) {
        for (TamanoFoto tamano : TamanoFoto.values()) {
            try {
                minio.removeObject(RemoveObjectArgs.builder().bucket(bucket).object(objeto(foto, tamano)).build());
            } catch (Exception e) {
                throw new IllegalStateException("No se pudo borrar la foto " + foto.id() + " de MinIO", e);
            }
        }
    }

    private static String objeto(Foto foto, TamanoFoto tamano) {
        return PREFIJO + foto.id() + "/" + tamano.name().toLowerCase(Locale.ROOT) + ".jpg";
    }
}
