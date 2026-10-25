package co.edu.unimagdalena.samariantour.recomendacion.recomendacion.dominio.excepcion;

/**
 * La fuente de alertas no respondió o respondió algo que no se pudo leer. No llega al turista: la caché conserva la
 * última lectura buena (DR-16). La causa es un texto corto para el log.
 */
public class FuenteAlertasNoDisponibleException extends RuntimeException {

    private final String causa;

    public FuenteAlertasNoDisponibleException(String causa) {
        super("Fuente de alertas no disponible: " + causa);
        this.causa = causa;
    }

    public String causa() {
        return causa;
    }
}
