package co.edu.unimagdalena.samariantour.nucleo.cuentas.dominio.puerto.salida;

/** Mensajes de WhatsApp de las cuentas (RF-03). */
public interface NotificationPort {

    /** El código de 6 dígitos que confirma que el número es del turista. */
    void enviarCodigoWhatsapp(String whatsapp, String codigo);
}
