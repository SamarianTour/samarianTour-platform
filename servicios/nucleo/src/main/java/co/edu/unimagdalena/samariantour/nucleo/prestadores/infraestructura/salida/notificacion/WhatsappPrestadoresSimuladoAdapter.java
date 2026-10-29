package co.edu.unimagdalena.samariantour.nucleo.prestadores.infraestructura.salida.notificacion;

import co.edu.unimagdalena.samariantour.nucleo.prestadores.dominio.puerto.salida.NotificacionPrestadorPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * WhatsApp simulado de los prestadores (DN-33), como el de las cuentas (DN-28): escribe en el log el número
 * enmascarado y el código, para poder verificarlo en la demo.
 */
@Component
public class WhatsappPrestadoresSimuladoAdapter implements NotificacionPrestadorPort {

    private static final Logger LOG = LoggerFactory.getLogger(WhatsappPrestadoresSimuladoAdapter.class);

    @Override
    public void enviarCodigo(String whatsapp, String codigo) {
        LOG.info("[WhatsApp simulado] para={} tipo=CODIGO_VERIFICACION_PRESTADOR codigo={}", enmascarar(whatsapp),
                codigo);
    }

    @Override
    public void avisarRetiroCatalogo(String whatsapp, String numeroRnt) {
        LOG.info("[WhatsApp simulado] para={} tipo=CATALOGO_RETIRADO_RNT rnt={}", enmascarar(whatsapp), numeroRnt);
    }

    /** Solo los últimos 4 dígitos: {@code +573001234567} queda {@code ******4567}. */
    static String enmascarar(String whatsapp) {
        String digitos = whatsapp == null ? "" : whatsapp.replaceAll("\\D", "");
        return digitos.length() <= 4 ? "****" : "******" + digitos.substring(digitos.length() - 4);
    }
}
