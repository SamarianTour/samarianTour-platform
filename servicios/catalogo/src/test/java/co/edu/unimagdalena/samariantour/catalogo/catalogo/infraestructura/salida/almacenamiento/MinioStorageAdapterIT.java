package co.edu.unimagdalena.samariantour.catalogo.catalogo.infraestructura.salida.almacenamiento;

import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.puerto.salida.FileStoragePort;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.MinIOContainer;
import org.testcontainers.utility.DockerImageName;

import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * MinIO real en Docker, con la misma versión que el piloto. Las pruebas están en el contrato, que también cumple la
 * carpeta local.
 */
class MinioStorageAdapterIT implements ContratoFileStorage {

    /** La misma imagen que Docker Compose: MinIO ya no publica la suya en Docker Hub (DC-39). */
    static final MinIOContainer MINIO = new MinIOContainer(
            DockerImageName.parse("cgr.dev/chainguard/minio:latest").asCompatibleSubstituteFor("minio/minio"));

    static {
        MINIO.start();
    }

    private final MinioStorageAdapter fotos = nuevoAdaptador();

    @Override
    public FileStoragePort fotos() {
        return fotos;
    }

    @Test
    @DisplayName("al arrancar crea el bucket si falta, y arrancar otra réplica con el bucket creado no falla")
    void bucket() {
        assertThatCode(() -> nuevoAdaptador().afterPropertiesSet()).doesNotThrowAnyException();
    }

    private static MinioStorageAdapter nuevoAdaptador() {
        MinioStorageAdapter adaptador = new MinioStorageAdapter(MINIO.getS3URL(), MINIO.getUserName(),
                MINIO.getPassword(), "catalogo-pruebas");
        adaptador.afterPropertiesSet();
        return adaptador;
    }
}
