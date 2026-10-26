package co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.excepcion;

/**
 * Solo el turista con WhatsApp verificado puede enviar solicitudes: es su único canal de notificación (RF-03, RF-30,
 * RF-44) [SUPUESTO el código] (DN-15).
 */
public class WhatsappNoVerificadoException extends ExcepcionDominio {

    public WhatsappNoVerificadoException() {
        super("WHATSAPP_NO_VERIFICADO",
                "Verifica tu WhatsApp antes de enviar una solicitud: por ahí recibes cada cambio");
    }
}
