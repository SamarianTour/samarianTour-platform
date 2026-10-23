package co.edu.unimagdalena.samariantour.catalogo.catalogo.infraestructura.entrada.rest;

import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.excepcion.DatoInvalidoException;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.UncheckedIOException;

/**
 * Lee la foto que llega en la parte {@code foto} de una petición multipart (DC-39). Spring ya rechazó un archivo de más
 * de 5 MB; el caso de uso revisa el resto.
 */
final class ArchivoSubido {

    private ArchivoSubido() {
    }

    static byte[] contenido(MultipartFile foto) {
        if (foto.isEmpty()) {
            throw new DatoInvalidoException("foto", "no llegó ninguna imagen");
        }
        try {
            return foto.getBytes();
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo leer la foto recibida", e);
        }
    }
}
