package co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.politica;

import co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.modelo.CanalNotificacion;
import co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.modelo.Destinatario;
import co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.modelo.EstadoSolicitud;
import co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.modelo.NotificacionSolicitud;
import co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.modelo.SolicitudDisponibilidad;
import co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.modelo.TipoNotificacion;
import co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.modelo.TransicionSolicitud;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Quién recibe qué en cada cambio de estado (DN-13):
 * <ul>
 *   <li>el turista, por WhatsApp, cada cambio de su solicitud, incluida su creación (RF-44); la de Atendida lleva el
 *   nombre de la oferta, el precio base y el enlace a la confirmación (RF-45), y la de Sin respuesta, el motivo
 *   (RF-41);</li>
 *   <li>el operador, en la plataforma, la llegada de cada solicitud a su cola (RF-34);</li>
 *   <li>el prestador, por WhatsApp, que el cupo apartado quedó libre cuando la solicitud vence o el turista la declina
 *   (RF-56), y los datos para coordinar cuando queda Pagada (RF-57): la misma SOLICITUD_PAGADA del turista, con la
 *   fecha, la hora y las personas de la solicitud; el nombre y el WhatsApp del turista se agregan al enviarla.</li>
 * </ul>
 * Las notificaciones llevan los datos de la plantilla, nunca números de teléfono: el WhatsApp se busca al enviar.
 */
public final class ReglaNotificaciones {

    private ReglaNotificaciones() {
    }

    public static List<NotificacionSolicitud> para(SolicitudDisponibilidad solicitud, TransicionSolicitud transicion) {
        List<NotificacionSolicitud> resultado = new ArrayList<>();
        Destinatario turista = Destinatario.turista(solicitud.turistaId());
        Destinatario prestador = Destinatario.prestador(solicitud.oferta().prestadorId());
        switch (transicion.a()) {
            case PENDIENTE -> {
                resultado.add(whatsapp(solicitud, transicion, turista, TipoNotificacion.SOLICITUD_RECIBIDA));
                resultado.add(notificacion(solicitud, transicion, Destinatario.operador(solicitud.operadorId()),
                        CanalNotificacion.PLATAFORMA, TipoNotificacion.NUEVA_SOLICITUD));
            }
            case ATENDIDA -> resultado.add(whatsapp(solicitud, transicion, turista,
                    TipoNotificacion.SOLICITUD_ATENDIDA));
            case NO_DISPONIBLE -> resultado.add(whatsapp(solicitud, transicion, turista,
                    TipoNotificacion.SOLICITUD_NO_DISPONIBLE));
            case SIN_RESPUESTA -> resultado.add(whatsapp(solicitud, transicion, turista,
                    TipoNotificacion.SOLICITUD_SIN_RESPUESTA));
            case CONFIRMADA -> resultado.add(whatsapp(solicitud, transicion, turista,
                    TipoNotificacion.SOLICITUD_CONFIRMADA));
            case DECLINADA -> {
                resultado.add(whatsapp(solicitud, transicion, turista, TipoNotificacion.SOLICITUD_DECLINADA));
                resultado.add(whatsapp(solicitud, transicion, prestador, TipoNotificacion.CUPO_LIBERADO));
            }
            case VENCIDA -> {
                resultado.add(whatsapp(solicitud, transicion, turista, TipoNotificacion.SOLICITUD_VENCIDA));
                resultado.add(whatsapp(solicitud, transicion, prestador, TipoNotificacion.CUPO_LIBERADO));
            }
            case PAGO_FALLIDO -> resultado.add(whatsapp(solicitud, transicion, turista,
                    TipoNotificacion.PAGO_FALLIDO));
            case PAGADA -> {
                resultado.add(whatsapp(solicitud, transicion, turista, TipoNotificacion.SOLICITUD_PAGADA));
                resultado.add(whatsapp(solicitud, transicion, prestador, TipoNotificacion.SOLICITUD_PAGADA));
            }
        }
        return resultado;
    }

    private static NotificacionSolicitud whatsapp(SolicitudDisponibilidad solicitud, TransicionSolicitud transicion,
                                                  Destinatario destinatario, TipoNotificacion tipo) {
        return notificacion(solicitud, transicion, destinatario, CanalNotificacion.WHATSAPP, tipo);
    }

    private static NotificacionSolicitud notificacion(SolicitudDisponibilidad solicitud,
                                                      TransicionSolicitud transicion, Destinatario destinatario,
                                                      CanalNotificacion canal, TipoNotificacion tipo) {
        return new NotificacionSolicitud(UUID.randomUUID(), solicitud.id(), destinatario, canal, tipo,
                datos(solicitud, transicion), transicion.en());
    }

    /** Los parámetros de la plantilla, con el estado ya aplicado. */
    private static Map<String, String> datos(SolicitudDisponibilidad solicitud, TransicionSolicitud transicion) {
        Map<String, String> datos = new LinkedHashMap<>();
        datos.put("solicitudId", solicitud.id().toString());
        datos.put("nombreOferta", solicitud.oferta().nombre());
        datos.put("fechaInicio", solicitud.fechaInicio().toString());
        // Los campos del tipo de oferta (RF-30): la hora de salida o de la reserva, o la salida y las unidades.
        if (solicitud.horaSalida() != null) {
            datos.put("horaSalida", solicitud.horaSalida().toString());
        }
        if (solicitud.horaReserva() != null) {
            datos.put("horaReserva", solicitud.horaReserva().toString());
        }
        if (solicitud.fechaSalida() != null) {
            datos.put("fechaSalida", solicitud.fechaSalida().toString());
            datos.put("numeroUnidades", solicitud.numeroUnidades().toString());
        }
        datos.put("personas", Integer.toString(solicitud.personas()));
        datos.put("estado", transicion.a().name());
        if (solicitud.venceEn() != null) {
            datos.put("venceEn", solicitud.venceEn().toString());
        }
        if (solicitud.condiciones() != null && transicion.a() != EstadoSolicitud.DECLINADA) {
            datos.put("precioBase", Long.toString(solicitud.condiciones().precioBase()));
        }
        if (transicion.a() == EstadoSolicitud.ATENDIDA) {
            datos.put("politicaCancelacion", solicitud.condiciones().politicaCancelacion());
        }
        if (transicion.motivo() != null) {
            datos.put("motivo", transicion.motivo());
        }
        if (transicion.a() == EstadoSolicitud.PAGADA) {
            // Para que el envío busque el nombre y el WhatsApp del turista que recibe el prestador (RF-57).
            datos.put("turistaId", solicitud.turistaId().toString());
        }
        return datos;
    }
}
