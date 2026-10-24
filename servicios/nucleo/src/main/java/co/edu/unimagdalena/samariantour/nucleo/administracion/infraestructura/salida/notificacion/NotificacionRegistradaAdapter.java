package co.edu.unimagdalena.samariantour.nucleo.administracion.infraestructura.salida.notificacion;

import co.edu.unimagdalena.samariantour.nucleo.administracion.dominio.puerto.salida.NotificacionPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Notificación simulada (DN-08): mientras no exista el adaptador de WhatsApp ni los contactos de cada usuario, solo
 * registra en el log el mensaje que se enviaría. El de WhatsApp lo reemplaza sin tocar el dominio.
 */
@Component
public class NotificacionRegistradaAdapter implements NotificacionPort {

    private static final Logger LOG = LoggerFactory.getLogger(NotificacionRegistradaAdapter.class);

    @Override
    public void rechazoDeOferta(UUID prestadorId, String nombreOferta, String motivo) {
        LOG.info("[Notificación simulada] Al prestador {}: tu oferta \"{}\" no se aprobó. Motivo: {}",
                prestadorId, nombreOferta, motivo);
    }

    @Override
    public void rechazoDeOpinion(UUID turistaId, String motivo) {
        LOG.info("[Notificación simulada] Al turista {}: tu opinión no se publicó. Motivo: {}", turistaId, motivo);
    }
}
