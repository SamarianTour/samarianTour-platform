package co.edu.unimagdalena.samariantour.nucleo.cuentas.infraestructura.salida.notificacion;

import co.edu.unimagdalena.samariantour.nucleo.cuentas.dominio.puerto.salida.NotificationPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * WhatsApp simulado de las cuentas (DN-18), como el de las solicitudes (DN-13): mientras no exista la integración con
 * WhatsApp Business, escribe en el log el número enmascarado y el código, para poder verificarlo en la demo. El código
 * solo aparece aquí porque es simulado; un adaptador real nunca lo escribiría.
 */
@Component
public class WhatsappCuentasSimuladoAdapter implements NotificationPort {

    private static final Logger LOG = LoggerFactory.getLogger(WhatsappCuentasSimuladoAdapter.class);

    @Override
    public void enviarCodigoWhatsapp(String whatsapp, String codigo) {
        LOG.info("[WhatsApp simulado] para={} tipo=CODIGO_VERIFICACION codigo={}", enmascarar(whatsapp), codigo);
    }

    /** Solo los últimos 4 dígitos: {@code +573001234567} queda {@code ******4567}. */
    static String enmascarar(String whatsapp) {
        String digitos = whatsapp == null ? "" : whatsapp.replaceAll("\\D", "");
        return digitos.length() <= 4 ? "****" : "******" + digitos.substring(digitos.length() - 4);
    }
}
