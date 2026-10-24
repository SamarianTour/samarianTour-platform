package co.edu.unimagdalena.samariantour.recomendacion.recomendacion.dominio.excepcion;

/**
 * No hay ofertas con qué recomendar: el catálogo no responde y no queda ninguna copia en caché (DR-07). El resto de
 * la plataforma sigue funcionando (ADR-04).
 */
public class RecomendacionNoDisponibleException extends ExcepcionDominio {

    public RecomendacionNoDisponibleException(String detalle) {
        super("RECOMENDACION_NO_DISPONIBLE", detalle);
    }
}
