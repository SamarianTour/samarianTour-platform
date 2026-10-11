package co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.excepcion;

/**
 * La zona indicada no existe o está inactiva (RF-84).
 */
public class ZonaNoDisponibleException extends ExcepcionDominio {

    public ZonaNoDisponibleException(String zonaId) {
        super("ZONA_NO_DISPONIBLE", "La zona %s no existe o está inactiva".formatted(zonaId));
    }
}
