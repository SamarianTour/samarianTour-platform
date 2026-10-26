package co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.excepcion;

/**
 * El catálogo no respondió a tiempo al consultar la oferta, o su circuito está abierto (ADR-04, QAS-03): la solicitud
 * no se crea, sin estados intermedios, y el turista puede reintentar con la misma clave de idempotencia.
 */
public class CatalogoNoDisponibleException extends ExcepcionDominio {

    public CatalogoNoDisponibleException(String detalle) {
        super("CATALOGO_NO_DISPONIBLE", "El catálogo no está disponible: " + detalle
                + ". La solicitud no se creó; inténtalo de nuevo en unos segundos");
    }
}
