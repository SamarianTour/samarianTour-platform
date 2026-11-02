package co.edu.unimagdalena.samariantour.nucleo.indicadores.dominio.excepcion;

/** El catálogo no dio las ofertas elegibles en 3 s (RNF-05): la revisión del sesgo espera a la hora siguiente. */
public class CatalogoNoDisponibleException extends ExcepcionDominio {

    public CatalogoNoDisponibleException(String detalle) {
        super("CATALOGO_NO_DISPONIBLE", "El catálogo no está disponible: " + detalle);
    }
}
