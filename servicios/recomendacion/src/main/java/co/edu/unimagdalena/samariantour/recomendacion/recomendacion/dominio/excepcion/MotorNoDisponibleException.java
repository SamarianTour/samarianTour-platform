package co.edu.unimagdalena.samariantour.recomendacion.recomendacion.dominio.excepcion;

/**
 * Un motor de recomendación no pudo responder: tiempo agotado, límite del proveedor, error del servidor, respuesta
 * inválida o circuito abierto. No llega al usuario: el servicio responde con el motor por reglas (QAS-03, DR-05).
 * La causa es un texto corto para el log, nunca el contenido de la consulta.
 */
public class MotorNoDisponibleException extends RuntimeException {

    private final String causa;

    public MotorNoDisponibleException(String causa) {
        super("Motor de recomendación no disponible: " + causa);
        this.causa = causa;
    }

    public String causa() {
        return causa;
    }
}
