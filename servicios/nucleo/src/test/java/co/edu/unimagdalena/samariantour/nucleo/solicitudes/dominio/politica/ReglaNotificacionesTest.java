package co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.politica;

import co.edu.unimagdalena.samariantour.nucleo.solicitudes.SolicitudesDePrueba;
import co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.modelo.CamposDelTipo;
import co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.modelo.CanalNotificacion;
import co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.modelo.DecisionTurista;
import co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.modelo.Destinatario;
import co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.modelo.NotificacionSolicitud;
import co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.modelo.SolicitudDisponibilidad;
import co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.modelo.TipoNotificacion;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

import static co.edu.unimagdalena.samariantour.nucleo.solicitudes.SolicitudesDePrueba.AGENCIA;
import static co.edu.unimagdalena.samariantour.nucleo.solicitudes.SolicitudesDePrueba.AHORA;
import static co.edu.unimagdalena.samariantour.nucleo.solicitudes.SolicitudesDePrueba.MIERCOLES;
import static co.edu.unimagdalena.samariantour.nucleo.solicitudes.SolicitudesDePrueba.OPERADOR;
import static co.edu.unimagdalena.samariantour.nucleo.solicitudes.SolicitudesDePrueba.SIETE;
import static co.edu.unimagdalena.samariantour.nucleo.solicitudes.SolicitudesDePrueba.TURISTA;
import static co.edu.unimagdalena.samariantour.nucleo.solicitudes.SolicitudesDePrueba.atendida;
import static co.edu.unimagdalena.samariantour.nucleo.solicitudes.SolicitudesDePrueba.caminata;
import static co.edu.unimagdalena.samariantour.nucleo.solicitudes.SolicitudesDePrueba.condiciones;
import static co.edu.unimagdalena.samariantour.nucleo.solicitudes.SolicitudesDePrueba.confirmada;
import static co.edu.unimagdalena.samariantour.nucleo.solicitudes.SolicitudesDePrueba.pendiente;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

class ReglaNotificacionesTest {

    @Test
    @DisplayName("al crearla: el turista por WhatsApp y el operador en la plataforma (RF-34, RF-44)")
    void creacion() {
        SolicitudDisponibilidad solicitud = SolicitudDisponibilidad.crear(UUID.randomUUID(), TURISTA, caminata(),
                MIERCOLES, CamposDelTipo.actividad(SIETE), 2, null, OPERADOR, AHORA);

        assertThat(solicitud.extraerNotificaciones())
                .extracting(NotificacionSolicitud::destinatario, NotificacionSolicitud::canal,
                        NotificacionSolicitud::tipo)
                .containsExactly(
                        tuple(Destinatario.turista(TURISTA), CanalNotificacion.WHATSAPP,
                                TipoNotificacion.SOLICITUD_RECIBIDA),
                        tuple(Destinatario.operador(OPERADOR), CanalNotificacion.PLATAFORMA,
                                TipoNotificacion.NUEVA_SOLICITUD));
        assertThat(solicitud.extraerNotificaciones()).as("se entregan una sola vez").isEmpty();
    }

    @Test
    @DisplayName("atendida: el turista recibe la oferta, el precio base, la política, el plazo y el id para el enlace")
    void alAtenderla() {
        SolicitudDisponibilidad solicitud = pendiente();
        solicitud.atender(OPERADOR, condiciones(), AHORA.plus(Duration.ofHours(1)));

        NotificacionSolicitud notificacion = solicitud.extraerNotificaciones().getFirst();

        assertThat(notificacion.tipo()).isEqualTo(TipoNotificacion.SOLICITUD_ATENDIDA);
        assertThat(notificacion.destinatario()).isEqualTo(Destinatario.turista(TURISTA));
        assertThat(notificacion.datos())
                .containsEntry("nombreOferta", "Caminata a Cabo San Juan del Guía")
                .containsEntry("precioBase", "300000")
                .containsEntry("politicaCancelacion", "Sin reembolso a menos de 24 h")
                .containsEntry("venceEn", AHORA.plus(Duration.ofHours(4)).toString())
                .containsEntry("solicitudId", solicitud.id().toString())
                .containsEntry("horaSalida", "07:00");
    }

    @Test
    @DisplayName("declinar y vencer avisan al turista y al prestador que el cupo quedó libre (RF-56)")
    void cupoLiberado() {
        SolicitudDisponibilidad declinada = atendida();
        declinada.responder(TURISTA, DecisionTurista.DECLINAR, AHORA.plus(Duration.ofHours(2)));
        SolicitudDisponibilidad vencida = confirmada();
        vencida.vencer(AHORA.plus(Duration.ofHours(4)));

        assertThat(declinada.extraerNotificaciones()).extracting(NotificacionSolicitud::destinatario,
                        NotificacionSolicitud::tipo)
                .containsExactly(tuple(Destinatario.turista(TURISTA), TipoNotificacion.SOLICITUD_DECLINADA),
                        tuple(Destinatario.prestador(AGENCIA), TipoNotificacion.CUPO_LIBERADO));
        assertThat(vencida.extraerNotificaciones()).extracting(NotificacionSolicitud::tipo)
                .containsExactly(TipoNotificacion.SOLICITUD_VENCIDA, TipoNotificacion.CUPO_LIBERADO);
    }

    @Test
    @DisplayName("sin respuesta: solo el turista, con el motivo; no hubo cupo apartado que liberar (RF-41)")
    void sinRespuesta() {
        SolicitudDisponibilidad solicitud = pendiente();
        solicitud.vencer(AHORA.plus(Duration.ofHours(24)));

        assertThat(solicitud.extraerNotificaciones()).singleElement().satisfies(notificacion -> {
            assertThat(notificacion.tipo()).isEqualTo(TipoNotificacion.SOLICITUD_SIN_RESPUESTA);
            assertThat(notificacion.datos()).containsKey("motivo");
        });
    }

    @Test
    @DisplayName("pagada: el turista y el prestador, para coordinar (RF-44, RF-57); los datos nunca llevan teléfonos")
    void pagada() {
        SolicitudDisponibilidad solicitud = confirmada();
        solicitud.registrarPagoAprobado(AHORA.plus(Duration.ofHours(3)));

        List<NotificacionSolicitud> notificaciones = solicitud.extraerNotificaciones();

        assertThat(notificaciones).extracting(NotificacionSolicitud::tipo)
                .containsExactly(TipoNotificacion.SOLICITUD_PAGADA, TipoNotificacion.SOLICITUD_PAGADA);
        assertThat(notificaciones).extracting(notificacion -> notificacion.destinatario().tipo())
                .containsExactly(Destinatario.Tipo.TURISTA, Destinatario.Tipo.PRESTADOR);
        assertThat(notificaciones.get(1).datos()).containsEntry("fechaInicio", "2026-10-28")
                .containsEntry("horaSalida", "07:00").containsEntry("personas", "2")
                .containsEntry("turistaId", TURISTA.toString());
        assertThat(notificaciones).allSatisfy(notificacion -> assertThat(notificacion.datos().keySet())
                .noneMatch(clave -> clave.toLowerCase().contains("whatsapp")));
    }

    @Test
    @DisplayName("los datos llevan los campos del tipo: la hora de la reserva, o la salida y las unidades (RF-57)")
    void camposDelTipo() {
        SolicitudDisponibilidad cena = SolicitudDisponibilidad.crear(UUID.randomUUID(), TURISTA,
                SolicitudesDePrueba.cocinaCaribe(), MIERCOLES, CamposDelTipo.restaurante(LocalTime.of(19, 30)), 4,
                null, OPERADOR, AHORA);
        SolicitudDisponibilidad cabana = SolicitudDisponibilidad.crear(UUID.randomUUID(), TURISTA,
                SolicitudesDePrueba.cabana(), MIERCOLES, SolicitudesDePrueba.dosNoches(MIERCOLES), 2, null, OPERADOR,
                AHORA);

        assertThat(cena.extraerNotificaciones().getFirst().datos()).containsEntry("horaReserva", "19:30")
                .doesNotContainKeys("horaSalida", "fechaSalida", "numeroUnidades");
        assertThat(cabana.extraerNotificaciones().getFirst().datos()).containsEntry("fechaSalida", "2026-10-30")
                .containsEntry("numeroUnidades", "1").doesNotContainKeys("horaSalida", "horaReserva");
    }

    @Test
    @DisplayName("cada otro cambio notifica al turista: no disponible, confirmada y pago fallido")
    void cadaCambio() {
        SolicitudDisponibilidad noDisponible = pendiente();
        noDisponible.marcarNoDisponible(OPERADOR, null, AHORA.plusSeconds(60));
        SolicitudDisponibilidad confirmada = atendida();
        confirmada.responder(TURISTA, DecisionTurista.CONFIRMAR, AHORA.plus(Duration.ofHours(2)));
        List<NotificacionSolicitud> alConfirmar = confirmada.extraerNotificaciones();
        confirmada.marcarIntentoEnCurso(AHORA.plus(Duration.ofHours(2)));
        confirmada.registrarPagoRechazado(AHORA.plus(Duration.ofHours(2)));

        assertThat(noDisponible.extraerNotificaciones()).extracting(NotificacionSolicitud::tipo)
                .containsExactly(TipoNotificacion.SOLICITUD_NO_DISPONIBLE);
        assertThat(alConfirmar).extracting(NotificacionSolicitud::tipo)
                .containsExactly(TipoNotificacion.SOLICITUD_CONFIRMADA);
        assertThat(confirmada.extraerNotificaciones()).extracting(NotificacionSolicitud::tipo)
                .containsExactly(TipoNotificacion.PAGO_FALLIDO);
    }
}
