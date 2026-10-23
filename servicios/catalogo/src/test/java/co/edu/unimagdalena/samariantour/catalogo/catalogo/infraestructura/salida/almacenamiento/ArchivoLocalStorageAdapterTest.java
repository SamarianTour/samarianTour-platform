package co.edu.unimagdalena.samariantour.catalogo.catalogo.infraestructura.salida.almacenamiento;

import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.puerto.salida.FileStoragePort;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

/**
 * La carpeta local, en una carpeta temporal de cada prueba. Las pruebas están en el contrato, que también cumple MinIO.
 */
class ArchivoLocalStorageAdapterTest implements ContratoFileStorage {

    @TempDir
    Path carpeta;

    @Override
    public FileStoragePort fotos() {
        return new ArchivoLocalStorageAdapter(carpeta);
    }
}
