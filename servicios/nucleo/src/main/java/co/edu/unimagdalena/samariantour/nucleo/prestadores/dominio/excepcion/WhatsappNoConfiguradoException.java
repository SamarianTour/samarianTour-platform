package co.edu.unimagdalena.samariantour.nucleo.prestadores.dominio.excepcion;

/** El prestador pidió un código sin haber configurado su WhatsApp (RF-71). */
public class WhatsappNoConfiguradoException extends ExcepcionDominio {

    public WhatsappNoConfiguradoException() {
        super("WHATSAPP_NO_CONFIGURADO", "Primero configura tu WhatsApp con PUT /prestador/whatsapp");
    }
}
