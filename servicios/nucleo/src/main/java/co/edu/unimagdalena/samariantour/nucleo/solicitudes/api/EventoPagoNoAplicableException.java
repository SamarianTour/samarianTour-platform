package co.edu.unimagdalena.samariantour.nucleo.solicitudes.api;

/**
 * La solicitud no existe o su estado no admite el evento: por ejemplo, un pago sobre una solicitud que el turista
 * nunca confirmó, o un intento después de terminar la ventana.
 */
public class EventoPagoNoAplicableException extends RuntimeException {

    private final String codigo;

    public EventoPagoNoAplicableException(String codigo, String mensaje) {
        super(mensaje);
        this.codigo = codigo;
    }

    /** SOLICITUD_NO_ENCONTRADA, TRANSICION_INVALIDA o PLAZO_VENCIDO. */
    public String codigo() {
        return codigo;
    }
}
