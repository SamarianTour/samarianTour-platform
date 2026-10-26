package co.edu.unimagdalena.samariantour.nucleo.solicitudes.aplicacion;

import co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.modelo.ContactoTurista;
import co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.modelo.Destinatario;
import co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.modelo.NotificacionPendiente;
import co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.modelo.TipoNotificacion;
import co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.puerto.entrada.EnviarNotificacionesUseCase;
import co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.puerto.salida.ContactoPort;
import co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.puerto.salida.NotificacionRepositoryPort;
import co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.puerto.salida.NotificationPort;
import co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.puerto.salida.PrestadoresPort;
import co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.puerto.salida.TransaccionPort;

import java.time.Clock;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Envía por WhatsApp un lote de notificaciones guardadas (DN-13). El lote se toma y se actualiza en una transacción:
 * mientras dura, la otra réplica salta esas filas. El número se busca al enviar, no al guardar: la notificación nunca
 * copia datos de contacto. Al prestador de una solicitud Pagada se le agregan el nombre y el WhatsApp del turista para
 * coordinar (RF-57); la fecha, la hora y las personas ya vienen en la notificación.
 */
public class EnviarNotificacionesService implements EnviarNotificacionesUseCase {

    public static final int TAMANO_LOTE = 20;

    private final NotificacionRepositoryPort notificaciones;
    private final NotificationPort whatsapp;
    private final ContactoPort contactos;
    private final PrestadoresPort prestadores;
    private final TransaccionPort transaccion;
    private final Clock reloj;

    public EnviarNotificacionesService(NotificacionRepositoryPort notificaciones, NotificationPort whatsapp,
                                       ContactoPort contactos, PrestadoresPort prestadores,
                                       TransaccionPort transaccion, Clock reloj) {
        this.notificaciones = notificaciones;
        this.whatsapp = whatsapp;
        this.contactos = contactos;
        this.prestadores = prestadores;
        this.transaccion = transaccion;
        this.reloj = reloj;
    }

    @Override
    public int enviarLote() {
        return transaccion.ejecutar(() -> {
            List<NotificacionPendiente> lote = notificaciones.tomarPendientes(reloj.instant(), TAMANO_LOTE);
            lote.forEach(this::enviar);
            return lote.size();
        });
    }

    private void enviar(NotificacionPendiente notificacion) {
        Optional<String> numero = numeroDe(notificacion.destinatario());
        if (numero.isEmpty()) {
            notificacion.registrarFallo("El destinatario no tiene un WhatsApp registrado", reloj.instant());
        } else {
            try {
                whatsapp.enviar(numero.get(), notificacion.tipo(), notificacion.solicitudId(),
                        parametros(notificacion));
                notificacion.registrarEnvio(reloj.instant());
            } catch (RuntimeException e) {
                notificacion.registrarFallo(e.getClass().getSimpleName() + ": " + e.getMessage(), reloj.instant());
            }
        }
        notificaciones.guardar(notificacion);
    }

    private Optional<String> numeroDe(Destinatario destinatario) {
        return switch (destinatario.tipo()) {
            case TURISTA -> contactos.turista(destinatario.id()).map(ContactoTurista::whatsapp);
            case PRESTADOR -> prestadores.whatsapp(destinatario.id());
            // Los avisos del operador son de la plataforma y nunca quedan pendientes de WhatsApp.
            case OPERADOR -> Optional.empty();
        };
    }

    private Map<String, String> parametros(NotificacionPendiente notificacion) {
        Map<String, String> parametros = new LinkedHashMap<>(notificacion.datos());
        parametros.remove("turistaId");
        if (notificacion.tipo() == TipoNotificacion.SOLICITUD_PAGADA
                && notificacion.destinatario().tipo() == Destinatario.Tipo.PRESTADOR
                && notificacion.datos().containsKey("turistaId")) {
            contactos.turista(UUID.fromString(notificacion.datos().get("turistaId"))).ifPresent(turista -> {
                parametros.put("nombreTurista", turista.nombre());
                parametros.put("whatsappTurista", turista.whatsapp());
            });
        }
        return parametros;
    }
}
