package co.edu.unimagdalena.samariantour.nucleo.administracion.dominio.puerto.salida;

import java.util.UUID;

/**
 * Notifica a quien publicó que su oferta o su opinión se rechazó, con el motivo (RF-79, RF-80). El canal del diseño
 * es WhatsApp; mientras no exista, el adaptador solo lo registra (DN-08).
 */
public interface NotificacionPort {

    void rechazoDeOferta(UUID prestadorId, String nombreOferta, String motivo);

    void rechazoDeOpinion(UUID turistaId, String motivo);
}
