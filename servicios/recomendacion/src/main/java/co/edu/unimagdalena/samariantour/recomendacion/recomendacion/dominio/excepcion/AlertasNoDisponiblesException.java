package co.edu.unimagdalena.samariantour.recomendacion.recomendacion.dominio.excepcion;

/**
 * No hay ninguna lectura de alertas: la fuente no ha respondido desde que el servicio arrancó (DR-16). La sección de
 * alertas se omite y el resto sigue (ADR-04).
 */
public class AlertasNoDisponiblesException extends ExcepcionDominio {

    public AlertasNoDisponiblesException() {
        super("ALERTAS_NO_DISPONIBLES", "Las alertas del destino no están disponibles en este momento");
    }
}
