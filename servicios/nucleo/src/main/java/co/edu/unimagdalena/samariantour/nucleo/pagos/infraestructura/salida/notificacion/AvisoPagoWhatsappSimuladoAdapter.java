package co.edu.unimagdalena.samariantour.nucleo.pagos.infraestructura.salida.notificacion;

import co.edu.unimagdalena.samariantour.nucleo.pagos.dominio.modelo.TipoAvisoPago;
import co.edu.unimagdalena.samariantour.nucleo.pagos.dominio.puerto.salida.AvisoPagoPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * WhatsApp simulado de pagos, como el de las solicitudes (DN-13): escribe en el log el destinatario enmascarado y el
 * tipo de aviso. Nunca el número completo ni los parámetros, que llevan montos y cuentas.
 */
@Component
public class AvisoPagoWhatsappSimuladoAdapter implements AvisoPagoPort {

    private static final Logger LOG = LoggerFactory.getLogger(AvisoPagoWhatsappSimuladoAdapter.class);

    @Override
    public void enviar(String whatsapp, TipoAvisoPago tipo, Map<String, String> parametros) {
        LOG.info("[WhatsApp simulado] para={} tipo={} solicitud={}", enmascarar(whatsapp), tipo,
                parametros.get("solicitudId"));
    }

    static String enmascarar(String whatsapp) {
        String digitos = whatsapp == null ? "" : whatsapp.replaceAll("\\D", "");
        return digitos.length() <= 4 ? "****" : "******" + digitos.substring(digitos.length() - 4);
    }
}
