package co.edu.unimagdalena.samariantour.nucleo.solicitudes.infraestructura.salida.notificacion;

import co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.modelo.TipoNotificacion;
import co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.puerto.salida.NotificationPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;

/**
 * WhatsApp simulado (DN-13): mientras no exista la integración con la API de WhatsApp Business, escribe en el log el
 * destinatario enmascarado, con solo sus últimos 4 dígitos, y el tipo de mensaje. Nunca escribe el número completo ni
 * los parámetros, que pueden traer datos personales (el WhatsApp del turista que recibe el prestador, RF-57). La de
 * Atendida muestra el enlace a la confirmación (RF-45), que solo lleva el identificador de la solicitud.
 */
@Component
public class WhatsappSimuladoAdapter implements NotificationPort {

    private static final Logger LOG = LoggerFactory.getLogger(WhatsappSimuladoAdapter.class);

    private final String enlaceConfirmacion;

    /** @param enlaceConfirmacion la pantalla de confirmación del frontend, con {@code {id}} en lugar de la solicitud */
    public WhatsappSimuladoAdapter(@Value("${nucleo.solicitudes.enlace-confirmacion:"
            + "http://localhost:8080/turista/solicitudes/{id}}") String enlaceConfirmacion) {
        this.enlaceConfirmacion = enlaceConfirmacion;
    }

    @Override
    public void enviar(String whatsapp, TipoNotificacion tipo, UUID solicitudId, Map<String, String> parametros) {
        if (tipo == TipoNotificacion.SOLICITUD_ATENDIDA) {
            LOG.info("[WhatsApp simulado] para={} tipo={} solicitud={} enlace={}", enmascarar(whatsapp), tipo,
                    solicitudId, enlace(solicitudId));
        } else {
            LOG.info("[WhatsApp simulado] para={} tipo={} solicitud={}", enmascarar(whatsapp), tipo, solicitudId);
        }
    }

    /** El enlace directo a la confirmación que lleva la notificación de Atendida (RF-45). */
    String enlace(UUID solicitudId) {
        return enlaceConfirmacion.replace("{id}", solicitudId.toString());
    }

    /** Solo los últimos 4 dígitos: {@code +573001234567} queda {@code ******4567}. */
    static String enmascarar(String whatsapp) {
        String digitos = whatsapp == null ? "" : whatsapp.replaceAll("\\D", "");
        return digitos.length() <= 4 ? "****" : "******" + digitos.substring(digitos.length() - 4);
    }
}
