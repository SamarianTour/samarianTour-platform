package co.edu.unimagdalena.samariantour.nucleo.solicitudes.aplicacion;

import co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.modelo.ActorTransicion;
import co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.modelo.CamposDelTipo;
import co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.modelo.EstadoSolicitud;
import co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.modelo.NotificacionSolicitud;
import co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.modelo.SolicitudDisponibilidad;
import co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.modelo.TipoNotificacion;
import co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.modelo.TransicionSolicitud;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;

import static co.edu.unimagdalena.samariantour.nucleo.solicitudes.SolicitudesDePrueba.AGENCIA;
import static co.edu.unimagdalena.samariantour.nucleo.solicitudes.SolicitudesDePrueba.AHORA;
import static co.edu.unimagdalena.samariantour.nucleo.solicitudes.SolicitudesDePrueba.MIERCOLES;
import static co.edu.unimagdalena.samariantour.nucleo.solicitudes.SolicitudesDePrueba.OPERADOR;
import static co.edu.unimagdalena.samariantour.nucleo.solicitudes.SolicitudesDePrueba.TURISTA;
import static co.edu.unimagdalena.samariantour.nucleo.solicitudes.SolicitudesDePrueba.cabana;
import static co.edu.unimagdalena.samariantour.nucleo.solicitudes.SolicitudesDePrueba.confirmada;
import static co.edu.unimagdalena.samariantour.nucleo.solicitudes.SolicitudesDePrueba.dosNoches;
import static co.edu.unimagdalena.samariantour.nucleo.solicitudes.SolicitudesDePrueba.SIETE;
import static co.edu.unimagdalena.samariantour.nucleo.solicitudes.SolicitudesDePrueba.caminata;
import static org.assertj.core.api.Assertions.assertThat;

/** El cierre de las solicitudes no pagadas de un prestador sin RNT activo (RF-77, HU-ADM-02, DN-36). */
class CerrarSolicitudesSinRntServiceTest {

    /** Dentro de la ventana de la confirmada: el cierre no es un vencimiento. */
    private static final Instant CIERRE = AHORA.plus(Duration.ofHours(3));

    private final DoblesSolicitudes.Repositorio repositorio = new DoblesSolicitudes.Repositorio();
    private final DoblesSolicitudes.Notificaciones notificaciones = new DoblesSolicitudes.Notificaciones();
    private final DoblesSolicitudes.Directorio directorio = new DoblesSolicitudes.Directorio();

    @Test
    @DisplayName("la Pendiente queda No disponible y la Confirmada Vencida, con el cupo liberado; las de otro, igual")
    void cierra() {
        SolicitudDisponibilidad pendiente = guardar(pendienteDeOtroTurista());
        SolicitudDisponibilidad confirmada = guardar(confirmada());
        SolicitudDisponibilidad deOtro = guardar(SolicitudDisponibilidad.crear(UUID.randomUUID(), TURISTA, cabana(),
                MIERCOLES, dosNoches(MIERCOLES), 2, null, OPERADOR, AHORA));
        directorio.inhabilitados.add(AGENCIA);

        assertThat(servicio(50).cerrar()).isEqualTo(2);

        assertThat(estado(pendiente)).isEqualTo(EstadoSolicitud.NO_DISPONIBLE);
        assertThat(estado(confirmada)).isEqualTo(EstadoSolicitud.VENCIDA);
        assertThat(estado(deOtro)).isEqualTo(EstadoSolicitud.PENDIENTE);
        assertThat(repositorio.buscar(confirmada.id()).orElseThrow().historial().getLast())
                .extracting(TransicionSolicitud::actor, TransicionSolicitud::motivo)
                .containsExactly(ActorTransicion.SISTEMA, SolicitudDisponibilidad.MOTIVO_RNT_INACTIVO);
        assertThat(notificaciones.registradas).extracting(NotificacionSolicitud::tipo).containsExactly(
                TipoNotificacion.SOLICITUD_NO_DISPONIBLE, TipoNotificacion.SOLICITUD_VENCIDA,
                TipoNotificacion.CUPO_LIBERADO);
    }

    @Test
    @DisplayName("con un intento de pago en curso espera su resultado; liberado el intento, la cierra")
    void intentoEnCurso() {
        SolicitudDisponibilidad confirmada = confirmada();
        confirmada.marcarIntentoEnCurso(CIERRE.minus(Duration.ofMinutes(5)));
        guardar(confirmada);
        directorio.inhabilitados.add(AGENCIA);

        assertThat(servicio(50).cerrar()).isZero();
        assertThat(estado(confirmada)).isEqualTo(EstadoSolicitud.CONFIRMADA);

        SolicitudDisponibilidad leida = repositorio.buscar(confirmada.id()).orElseThrow();
        leida.liberarIntentoEnCurso(CIERRE);
        repositorio.actualizar(leida);

        assertThat(servicio(50).cerrar()).isEqualTo(1);
        assertThat(estado(confirmada)).isEqualTo(EstadoSolicitud.VENCIDA);
    }

    @Test
    @DisplayName("cierra lote tras lote hasta terminar; con el RNT activo no toca ninguna")
    void lotes() {
        for (int i = 0; i < 5; i++) {
            guardar(pendienteDeOtroTurista());
        }

        assertThat(servicio(2).cerrar()).isZero();

        directorio.inhabilitados.add(AGENCIA);
        assertThat(servicio(2).cerrar()).isEqualTo(5);
        assertThat(repositorio.prestadoresConAbiertas()).isEmpty();
    }

    /** Una Pendiente de la caminata de la agencia: cada turista puede tener una abierta para la misma salida. */
    private static SolicitudDisponibilidad pendienteDeOtroTurista() {
        return SolicitudDisponibilidad.crear(UUID.randomUUID(), UUID.randomUUID(), caminata(), MIERCOLES,
                CamposDelTipo.actividad(SIETE), 2, null, OPERADOR, AHORA);
    }

    private SolicitudDisponibilidad guardar(SolicitudDisponibilidad solicitud) {
        solicitud.extraerNotificaciones();
        repositorio.insertar(solicitud);
        return solicitud;
    }

    private EstadoSolicitud estado(SolicitudDisponibilidad solicitud) {
        return repositorio.buscar(solicitud.id()).orElseThrow().estado();
    }

    private CerrarSolicitudesSinRntService servicio(int tamanoLote) {
        return new CerrarSolicitudesSinRntService(repositorio, notificaciones, directorio,
                new DoblesSolicitudes.TransaccionDirecta(), Clock.fixed(CIERRE, ZoneOffset.UTC), tamanoLote);
    }
}
