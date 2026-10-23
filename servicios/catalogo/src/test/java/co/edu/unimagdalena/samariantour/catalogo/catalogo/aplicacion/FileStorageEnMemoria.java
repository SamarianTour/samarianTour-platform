package co.edu.unimagdalena.samariantour.catalogo.catalogo.aplicacion;

import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.excepcion.DatoInvalidoException;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.Foto;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.TamanoFoto;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.puerto.salida.FileStoragePort;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Doble del almacenamiento de fotos: guarda los bytes tal cual, sin reprocesarlos. Rechaza lo que empieza por
 * {@link #NO_ES_IMAGEN}, para probar qué hace el caso de uso cuando el almacenamiento rechaza la imagen.
 */
class FileStorageEnMemoria implements FileStoragePort {

    static final byte NO_ES_IMAGEN = 0;

    private final Map<Foto, byte[]> fotos = new HashMap<>();

    @Override
    public void guardar(Foto foto, byte[] imagen) {
        if (imagen[0] == NO_ES_IMAGEN) {
            throw new DatoInvalidoException("foto", "debe ser una imagen JPG, PNG o WebP");
        }
        fotos.put(foto, imagen);
    }

    @Override
    public Optional<byte[]> leer(Foto foto, TamanoFoto tamano) {
        return Optional.ofNullable(fotos.get(foto));
    }

    @Override
    public void eliminar(Foto foto) {
        fotos.remove(foto);
    }

    boolean tiene(Foto foto) {
        return fotos.containsKey(foto);
    }

    int cantidad() {
        return fotos.size();
    }
}
