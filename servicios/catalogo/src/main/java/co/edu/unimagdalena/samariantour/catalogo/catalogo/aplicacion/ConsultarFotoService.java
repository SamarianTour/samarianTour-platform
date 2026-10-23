package co.edu.unimagdalena.samariantour.catalogo.catalogo.aplicacion;

import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.excepcion.DatoInvalidoException;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.excepcion.FotoNoEncontradaException;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.Foto;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.TamanoFoto;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.puerto.entrada.ConsultarFotoUseCase;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.puerto.salida.FileStoragePort;

/**
 * Entrega una foto guardada (DC-39). Un identificador mal formado responde igual que uno que no existe.
 */
public class ConsultarFotoService implements ConsultarFotoUseCase {

    private final FileStoragePort fotos;

    public ConsultarFotoService(FileStoragePort fotos) {
        this.fotos = fotos;
    }

    @Override
    public byte[] consultar(String fotoId, TamanoFoto tamano) {
        Foto foto;
        try {
            foto = new Foto(fotoId);
        } catch (DatoInvalidoException e) {
            throw new FotoNoEncontradaException(fotoId);
        }
        return fotos.leer(foto, tamano).orElseThrow(() -> new FotoNoEncontradaException(fotoId));
    }
}
