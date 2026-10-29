package co.edu.unimagdalena.samariantour.nucleo.pagos.dominio.excepcion;

/** La solicitud del turista no está para pagar: solo se paga Confirmada o con el pago fallido (RF-46, RF-58). */
public class SolicitudNoCobrableException extends ExcepcionDominio {

    public SolicitudNoCobrableException(String estado) {
        super("SOLICITUD_NO_COBRABLE", "La solicitud está " + estado
                + ": solo se paga una solicitud Confirmada o con el pago fallido");
    }
}
