package co.edu.unimagdalena.samariantour.nucleo.administracion.dominio.excepcion;

/**
 * El catálogo no respondió a tiempo al verificar lo que se va a moderar (DN-08): no se decide sin verificar, y el
 * administrador lo intenta de nuevo.
 */
public class CatalogoNoDisponibleException extends ExcepcionDominio {

    public CatalogoNoDisponibleException(String detalle) {
        super("CATALOGO_NO_DISPONIBLE", "El catálogo no está disponible: " + detalle);
    }
}
