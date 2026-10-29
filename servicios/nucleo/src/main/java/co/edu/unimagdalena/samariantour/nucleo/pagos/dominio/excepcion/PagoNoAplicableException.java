package co.edu.unimagdalena.samariantour.nucleo.pagos.dominio.excepcion;

/**
 * La solicitud no admitió el evento del pago: por ejemplo, la ventana terminó mientras el turista abría el pago
 * ({@code PLAZO_VENCIDO}). Lleva el código que respondió el módulo de solicitudes.
 */
public class PagoNoAplicableException extends ExcepcionDominio {

    public PagoNoAplicableException(String codigo, String mensaje) {
        super(codigo, mensaje);
    }
}
