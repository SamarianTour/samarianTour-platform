package co.edu.unimagdalena.samariantour.nucleo.solicitudes.aplicacion;

import co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.excepcion.ExcepcionDominio;
import co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.modelo.DecisionTurista;
import co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.modelo.EstadoSolicitud;
import co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.modelo.NotificacionSolicitud;
import co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.modelo.PeticionIdempotente;
import co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.modelo.RespuestaGuardada;
import co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.modelo.ResultadoIdempotente;
import co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.modelo.SolicitudDisponibilidad;
import co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.modelo.TipoNotificacion;
import co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.puerto.entrada.comando.ResponderCondicionesCommand;
import org.assertj.core.api.ThrowableAssert;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.util.UUID;

import static co.edu.unimagdalena.samariantour.nucleo.solicitudes.SolicitudesDePrueba.AHORA;
import static co.edu.unimagdalena.samariantour.nucleo.solicitudes.SolicitudesDePrueba.OTRO_TURISTA;
import static co.edu.unimagdalena.samariantour.nucleo.solicitudes.SolicitudesDePrueba.TURISTA;
import static co.edu.unimagdalena.samariantour.nucleo.solicitudes.SolicitudesDePrueba.atendida;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ResponderCondicionesServiceTest {

    private final DoblesSolicitudes.Repositorio repositorio = new DoblesSolicitudes.Repositorio();
    private final DoblesSolicitudes.Notificaciones notificaciones = new DoblesSolicitudes.Notificaciones();
    private final DoblesSolicitudes.Claves claves = new DoblesSolicitudes.Claves();
    private final SolicitudDisponibilidad solicitud = atendida();

    @Test
    @DisplayName("confirmar: Confirmada, guardada y notificada al turista")
    void confirmar() {
        repositorio.insertar(solicitud);

        responder(UUID.randomUUID(), TURISTA, DecisionTurista.CONFIRMAR, enLaVentana());

        assertThat(repositorio.buscar(solicitud.id()).orElseThrow().estado()).isEqualTo(EstadoSolicitud.CONFIRMADA);
        assertThat(notificaciones.registradas).extracting(NotificacionSolicitud::tipo)
                .containsExactly(TipoNotificacion.SOLICITUD_CONFIRMADA);
    }

    @Test
    @DisplayName("declinar: Declinada, y el prestador sabe que el cupo quedó libre (RF-56)")
    void declinar() {
        repositorio.insertar(solicitud);

        responder(UUID.randomUUID(), TURISTA, DecisionTurista.DECLINAR, enLaVentana());

        assertThat(repositorio.buscar(solicitud.id()).orElseThrow().estado()).isEqualTo(EstadoSolicitud.DECLINADA);
        assertThat(notificaciones.registradas).extracting(NotificacionSolicitud::tipo)
                .containsExactly(TipoNotificacion.SOLICITUD_DECLINADA, TipoNotificacion.CUPO_LIBERADO);
    }

    @Test
    @DisplayName("repetir la confirmación con la misma clave devuelve la misma respuesta y no la toca otra vez")
    void mismaClave() {
        repositorio.insertar(solicitud);
        UUID clave = UUID.randomUUID();
        ResultadoIdempotente primera = responder(clave, TURISTA, DecisionTurista.CONFIRMAR, enLaVentana());

        ResultadoIdempotente segunda = responder(clave, TURISTA, DecisionTurista.CONFIRMAR, enLaVentana());

        assertThat(segunda.repetida()).isTrue();
        assertThat(segunda.respuesta()).isEqualTo(primera.respuesta());
        assertThat(repositorio.actualizaciones).isEqualTo(1);
    }

    @Test
    @DisplayName("con otra clave, confirmar otra vez es TRANSICION_INVALIDA; la de otro turista, no encontrada")
    void invalidas() {
        repositorio.insertar(solicitud);
        responder(UUID.randomUUID(), TURISTA, DecisionTurista.CONFIRMAR, enLaVentana());

        assertCodigo(() -> responder(UUID.randomUUID(), TURISTA, DecisionTurista.CONFIRMAR, enLaVentana()),
                "TRANSICION_INVALIDA");
        assertCodigo(() -> responder(UUID.randomUUID(), OTRO_TURISTA, DecisionTurista.DECLINAR, enLaVentana()),
                "SOLICITUD_NO_ENCONTRADA");
        assertCodigo(() -> new ResponderCondicionesCommand(TURISTA, solicitud.id(), null), "DATO_INVALIDO");
    }

    @Test
    @DisplayName("terminada la ventana: PLAZO_VENCIDO, aunque el vencimiento todavía no haya corrido")
    void fueraDeLaVentana() {
        repositorio.insertar(solicitud);

        assertCodigo(() -> responder(UUID.randomUUID(), TURISTA, DecisionTurista.CONFIRMAR,
                DoblesSolicitudes.reloj(AHORA.plus(Duration.ofHours(4)))), "PLAZO_VENCIDO");
    }

    private ResultadoIdempotente responder(UUID clave, UUID turista, DecisionTurista decision, Clock reloj) {
        ResponderCondicionesService servicio = new ResponderCondicionesService(repositorio, notificaciones,
                new EjecucionIdempotente(claves, new DoblesSolicitudes.TransaccionDirecta(),
                        DoblesSolicitudes.sinEspera(), reloj), reloj);
        ResponderCondicionesCommand comando = new ResponderCondicionesCommand(turista, solicitud.id(), decision);
        return servicio.responder(comando, PeticionIdempotente.de(clave, turista, "RESPONDER_CONDICIONES",
                comando.toString()), cambiada -> new RespuestaGuardada(200, cambiada.estado().name()));
    }

    private static Clock enLaVentana() {
        return DoblesSolicitudes.reloj(AHORA.plus(Duration.ofHours(2)));
    }

    private static void assertCodigo(ThrowableAssert.ThrowingCallable accion, String codigo) {
        assertThatThrownBy(accion).isInstanceOf(ExcepcionDominio.class)
                .extracting(e -> ((ExcepcionDominio) e).codigo()).isEqualTo(codigo);
    }
}
