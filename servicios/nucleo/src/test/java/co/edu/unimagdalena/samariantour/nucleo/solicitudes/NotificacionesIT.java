package co.edu.unimagdalena.samariantour.nucleo.solicitudes;

import co.edu.unimagdalena.samariantour.nucleo.IntegracionPostgresBase;
import co.edu.unimagdalena.samariantour.nucleo.solicitudes.aplicacion.EnviarNotificacionesService;
import co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.modelo.AvisoPlataforma;
import co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.modelo.CamposDelTipo;
import co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.modelo.DecisionTurista;
import co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.modelo.Paginacion;
import co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.modelo.SolicitudDisponibilidad;
import co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.modelo.TipoNotificacion;
import co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.puerto.salida.AvisoPlataformaPort;
import co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.puerto.salida.ContactoPort;
import co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.puerto.salida.NotificacionRepositoryPort;
import co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.puerto.salida.NotificationPort;
import co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.puerto.salida.PrestadoresPort;
import co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.puerto.salida.SolicitudRepositoryPort;
import co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.puerto.salida.TransaccionPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

import static co.edu.unimagdalena.samariantour.nucleo.solicitudes.SolicitudesDePrueba.AHORA;
import static co.edu.unimagdalena.samariantour.nucleo.solicitudes.SolicitudesDePrueba.MIERCOLES;
import static co.edu.unimagdalena.samariantour.nucleo.solicitudes.SolicitudesDePrueba.OPERADOR;
import static co.edu.unimagdalena.samariantour.nucleo.solicitudes.SolicitudesDePrueba.SIETE;
import static co.edu.unimagdalena.samariantour.nucleo.solicitudes.SolicitudesDePrueba.TURISTA;
import static co.edu.unimagdalena.samariantour.nucleo.solicitudes.SolicitudesDePrueba.caminata;
import static co.edu.unimagdalena.samariantour.nucleo.solicitudes.SolicitudesDePrueba.condiciones;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Las notificaciones contra PostgreSQL (DN-13): se guardan en la transacción del cambio, solo existen si el cambio se
 * confirmó y la tarea las envía con el WhatsApp de la tabla de la demo.
 */
@SpringBootTest(properties = {"nucleo.bandeja.publicacion.cron=-", "nucleo.solicitudes.vencimiento.cron=-",
        "nucleo.solicitudes.notificaciones.cron=-"})
@ActiveProfiles("demo")
class NotificacionesIT extends IntegracionPostgresBase {

    @Autowired
    private SolicitudRepositoryPort solicitudes;

    @Autowired
    private NotificacionRepositoryPort notificaciones;

    @Autowired
    private AvisoPlataformaPort avisos;

    @Autowired
    private ContactoPort contactos;

    @Autowired
    private PrestadoresPort prestadores;

    @Autowired
    private TransaccionPort transaccion;

    @Autowired
    private JdbcTemplate jdbc;

    private final List<Enviado> enviados = new CopyOnWriteArrayList<>();

    record Enviado(String whatsapp, TipoNotificacion tipo, Map<String, String> parametros) {
    }

    @BeforeEach
    void vaciar() {
        jdbc.update("DELETE FROM notificaciones_solicitud");
        jdbc.update("DELETE FROM transiciones_solicitud");
        jdbc.update("DELETE FROM solicitudes");
        enviados.clear();
    }

    @Test
    @DisplayName("si el cambio se deshace, sus notificaciones tampoco quedan")
    void soloSiSeConfirma() {
        assertThatThrownBy(() -> transaccion.ejecutar(() -> {
            guardarNueva();
            throw new IllegalStateException("algo falló después de guardar");
        })).isInstanceOf(IllegalStateException.class);

        assertThat(contar()).isZero();
        transaccion.ejecutar(this::guardarNueva);
        assertThat(contar()).isEqualTo(2);
    }

    @Test
    @DisplayName("la del operador es un aviso de la plataforma, ya entregado; la del turista espera el envío")
    void avisoDelOperador() {
        SolicitudDisponibilidad solicitud = transaccion.ejecutar(this::guardarNueva);

        List<AvisoPlataforma> delOperador = avisos.avisosDe(OPERADOR, new Paginacion(0, 10)).contenido();

        assertThat(delOperador).singleElement().satisfies(aviso -> {
            assertThat(aviso.tipo()).isEqualTo(TipoNotificacion.NUEVA_SOLICITUD);
            assertThat(aviso.solicitudId()).isEqualTo(solicitud.id());
            assertThat(aviso.datos()).containsEntry("nombreOferta", "Caminata a Cabo San Juan del Guía");
        });
        assertThat(jdbc.queryForObject("SELECT estado FROM notificaciones_solicitud WHERE canal = 'PLATAFORMA'",
                String.class)).isEqualTo("ENVIADA");
        assertThat(jdbc.queryForObject("SELECT estado FROM notificaciones_solicitud WHERE canal = 'WHATSAPP'",
                String.class)).isEqualTo("PENDIENTE");
    }

    @Test
    @DisplayName("el envío busca el WhatsApp de la demo y marca la notificación como enviada; solo una vez")
    void envio() {
        transaccion.ejecutar(this::guardarNueva);

        assertThat(servicio(capturando()).enviarLote()).isEqualTo(1);
        assertThat(servicio(capturando()).enviarLote()).isZero();

        assertThat(enviados).singleElement().satisfies(enviado -> {
            assertThat(enviado.whatsapp()).isEqualTo("+573105550001");
            assertThat(enviado.tipo()).isEqualTo(TipoNotificacion.SOLICITUD_RECIBIDA);
        });
        assertThat(jdbc.queryForObject("SELECT estado FROM notificaciones_solicitud WHERE canal = 'WHATSAPP'",
                String.class)).isEqualTo("ENVIADA");
    }

    @Test
    @DisplayName("SOLICITUD_PAGADA: al prestador, el nombre, la fecha, la hora, las personas y el WhatsApp del turista "
            + "(RF-57), que no se guardan")
    void solicitudPagada() {
        SolicitudDisponibilidad solicitud = transaccion.ejecutar(this::guardarNueva);
        transaccion.ejecutar(() -> {
            SolicitudDisponibilidad leida = solicitudes.buscar(solicitud.id()).orElseThrow();
            leida.atender(OPERADOR, condiciones(), AHORA.plus(Duration.ofHours(1)));
            leida.responder(TURISTA, DecisionTurista.CONFIRMAR, AHORA.plus(Duration.ofHours(2)));
            leida.registrarPagoAprobado(AHORA.plus(Duration.ofHours(3)));
            solicitudes.actualizar(leida);
            notificaciones.registrar(leida.extraerNotificaciones());
            return null;
        });

        while (servicio(capturando()).enviarLote() > 0) {
            // Envía todo lo pendiente.
        }

        assertThat(enviados).filteredOn(enviado -> enviado.tipo() == TipoNotificacion.SOLICITUD_PAGADA)
                .extracting(Enviado::whatsapp).containsExactlyInAnyOrder("+573105550001", "+573001110101");
        assertThat(enviados).filteredOn(enviado -> enviado.whatsapp().equals("+573001110101"))
                .singleElement().satisfies(enviado -> {
                    assertThat(enviado.tipo()).isEqualTo(TipoNotificacion.SOLICITUD_PAGADA);
                    assertThat(enviado.parametros()).containsEntry("nombreTurista", "Laura Gómez")
                            .containsEntry("whatsappTurista", "+573105550001")
                            .containsEntry("fechaInicio", solicitud.fechaInicio().toString())
                            .containsEntry("horaSalida", "07:00").containsEntry("personas", "2")
                            .doesNotContainKey("turistaId");
                });
        assertThat(enviados).filteredOn(enviado -> enviado.whatsapp().equals("+573105550001")
                        && enviado.tipo() == TipoNotificacion.SOLICITUD_PAGADA)
                .singleElement().satisfies(enviado -> assertThat(enviado.parametros())
                        .doesNotContainKeys("nombreTurista", "whatsappTurista", "turistaId"));
        assertThat(jdbc.queryForList("SELECT datos::text FROM notificaciones_solicitud", String.class))
                .noneMatch(datos -> datos.contains("+57"));
    }

    @Test
    @DisplayName("si el envío falla, la notificación se reprograma y no se pierde")
    void falla() {
        transaccion.ejecutar(this::guardarNueva);

        servicio((whatsapp, tipo, solicitudId, parametros) -> {
            throw new IllegalStateException("WhatsApp no respondió");
        }).enviarLote();

        assertThat(jdbc.queryForMap("SELECT estado, intentos, ultimo_error FROM notificaciones_solicitud "
                + "WHERE canal = 'WHATSAPP'"))
                .containsEntry("estado", "PENDIENTE").containsEntry("intentos", 1)
                .hasEntrySatisfying("ultimo_error", error -> assertThat((String) error).contains("no respondió"));
    }

    private SolicitudDisponibilidad guardarNueva() {
        SolicitudDisponibilidad solicitud = SolicitudDisponibilidad.crear(UUID.randomUUID(), TURISTA, caminata(),
                MIERCOLES, CamposDelTipo.actividad(SIETE), 2, null, OPERADOR, AHORA);
        solicitudes.insertar(solicitud);
        notificaciones.registrar(solicitud.extraerNotificaciones());
        return solicitud;
    }

    private NotificationPort capturando() {
        return (whatsapp, tipo, solicitudId, parametros) -> enviados.add(new Enviado(whatsapp, tipo, parametros));
    }

    private EnviarNotificacionesService servicio(NotificationPort whatsapp) {
        return new EnviarNotificacionesService(notificaciones, whatsapp, contactos, prestadores, transaccion,
                Clock.fixed(Instant.parse("2026-10-24T20:00:00Z"), ZoneOffset.UTC));
    }

    private int contar() {
        return jdbc.queryForObject("SELECT count(*) FROM notificaciones_solicitud", Integer.class);
    }
}
