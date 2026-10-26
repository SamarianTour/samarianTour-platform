package co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.puerto.salida;

import co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.modelo.TipoNotificacion;

import java.util.Map;
import java.util.UUID;

/**
 * El envío por WhatsApp, el canal único del turista y del prestador (RF-44, RF-71). Cada tipo es una plantilla con
 * sus parámetros. Mientras no exista la integración con WhatsApp, el adaptador es simulado (DN-13).
 */
public interface NotificationPort {

    /**
     * @param whatsapp   el número del destinatario
     * @param parametros los datos de la plantilla
     * @throws RuntimeException si el envío falla; se reintenta más tarde
     */
    void enviar(String whatsapp, TipoNotificacion tipo, UUID solicitudId, Map<String, String> parametros);
}
