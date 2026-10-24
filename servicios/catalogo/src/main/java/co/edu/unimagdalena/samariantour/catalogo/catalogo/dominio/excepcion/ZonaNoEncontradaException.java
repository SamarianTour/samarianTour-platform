package co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.excepcion;

public class ZonaNoEncontradaException extends ExcepcionDominio {

    public ZonaNoEncontradaException(String zonaId) {
        super("ZONA_NO_ENCONTRADA", "No existe la zona %s".formatted(zonaId));
    }
}
