package co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.excepcion;

/**
 * No hay una foto guardada con ese identificador.
 */
public class FotoNoEncontradaException extends ExcepcionDominio {

    public FotoNoEncontradaException(String fotoId) {
        super("FOTO_NO_ENCONTRADA", "No existe la foto %s".formatted(fotoId));
    }
}
