package co.edu.unimagdalena.samariantour.nucleo.prestadores.dominio.puerto.salida;

/** Los mensajes de WhatsApp al prestador (RF-71, RF-78). */
public interface NotificacionPrestadorPort {

    /** El código de 6 dígitos que confirma que el número es suyo. */
    void enviarCodigo(String whatsapp, String codigo);

    /** Su catálogo salió de la vista pública porque su RNT dejó de estar activo (RF-78). */
    void avisarRetiroCatalogo(String whatsapp, String numeroRnt);
}
