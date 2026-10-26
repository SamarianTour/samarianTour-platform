package co.edu.unimagdalena.samariantour.nucleo.solicitudes;

import co.edu.unimagdalena.samariantour.nucleo.IntegracionPostgresBase;
import co.edu.unimagdalena.samariantour.nucleo.compartido.concurrencia.EjecutorConReintento;
import co.edu.unimagdalena.samariantour.nucleo.solicitudes.aplicacion.AtenderSolicitudService;
import co.edu.unimagdalena.samariantour.nucleo.solicitudes.aplicacion.VencerSolicitudesService;
import co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.excepcion.TransicionInvalidaException;
import co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.modelo.CamposDelTipo;
import co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.modelo.EstadoSolicitud;
import co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.modelo.NotificacionPendiente;
import co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.modelo.NotificacionSolicitud;
import co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.modelo.Pagina;
import co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.modelo.Paginacion;
import co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.modelo.ResultadoAtencion;
import co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.modelo.SolicitudDisponibilidad;
import co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.puerto.entrada.comando.AtenderSolicitudCommand;
import co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.puerto.salida.NotificacionRepositoryPort;
import co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.puerto.salida.SolicitudRepositoryPort;
import co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.puerto.salida.TransaccionPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static co.edu.unimagdalena.samariantour.nucleo.solicitudes.SolicitudesDePrueba.AHORA;
import static co.edu.unimagdalena.samariantour.nucleo.solicitudes.SolicitudesDePrueba.MIERCOLES;
import static co.edu.unimagdalena.samariantour.nucleo.solicitudes.SolicitudesDePrueba.OPERADOR;
import static co.edu.unimagdalena.samariantour.nucleo.solicitudes.SolicitudesDePrueba.SIETE;
import static co.edu.unimagdalena.samariantour.nucleo.solicitudes.SolicitudesDePrueba.TURISTA;
import static co.edu.unimagdalena.samariantour.nucleo.solicitudes.SolicitudesDePrueba.caminata;
import static co.edu.unimagdalena.samariantour.nucleo.solicitudes.SolicitudesDePrueba.confirmada;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Los vencimientos contra PostgreSQL (DN-14): dos réplicas sin pisarse, la gracia del pago y el cruce con el operador.
 */
@SpringBootTest(properties = {"nucleo.bandeja.publicacion.cron=-", "nucleo.solicitudes.vencimiento.cron=-",
        "nucleo.solicitudes.notificaciones.cron=-"})
class VencimientosIT extends IntegracionPostgresBase {

    private static final Instant PASADAS_24_H = AHORA.plus(Duration.ofHours(24)).plusSeconds(1);

    @Autowired
    private SolicitudRepositoryPort solicitudes;

    @Autowired
    private NotificacionRepositoryPort notificaciones;

    @Autowired
    private TransaccionPort transaccion;

    @Autowired
    private JdbcTemplate jdbc;

    @BeforeEach
    void vaciar() {
        jdbc.update("DELETE FROM notificaciones_solicitud");
        jdbc.update("DELETE FROM transiciones_solicitud");
        jdbc.update("DELETE FROM solicitudes");
    }

    @Test
    @DisplayName("dos réplicas a la vez, con SKIP LOCKED: cada una vence lo suyo y ninguna vence dos veces la misma")
    void dosReplicas() throws Exception {
        for (int i = 0; i < 6; i++) {
            guardar(pendiente(MIERCOLES.plusDays(i)));
        }
        CountDownLatch salida = new CountDownLatch(1);
        NotificacionRepositoryPort lentas = new NotificacionesLentas(notificaciones, Duration.ofMillis(150));
        VencerSolicitudesService replicaA = vencedor(lentas, PASADAS_24_H, 3);
        VencerSolicitudesService replicaB = vencedor(lentas, PASADAS_24_H, 3);

        CompletableFuture<Integer> a = CompletableFuture.supplyAsync(() -> esperarYVencer(salida, replicaA));
        CompletableFuture<Integer> b = CompletableFuture.supplyAsync(() -> esperarYVencer(salida, replicaB));
        salida.countDown();

        assertThat(a.get(10, TimeUnit.SECONDS) + b.get(10, TimeUnit.SECONDS)).isEqualTo(6);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM solicitudes WHERE estado = 'SIN_RESPUESTA'",
                Integer.class)).isEqualTo(6);
        assertThat(jdbc.queryForList("SELECT count(*) FROM transiciones_solicitud GROUP BY solicitud_id",
                Integer.class)).as("la creación y un solo vencimiento por solicitud").containsOnly(2);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM notificaciones_solicitud "
                + "WHERE tipo = 'SOLICITUD_SIN_RESPUESTA'", Integer.class)).isEqualTo(6);
        assertThat(vencedor(notificaciones, PASADAS_24_H, 50).vencerLote()).as("ya no queda nada").isZero();
    }

    @Test
    @DisplayName("solo vence lo que ya toca: una pendiente con plazo vigente sigue Pendiente")
    void soloLoQueToca() {
        guardar(pendiente(MIERCOLES));

        assertThat(vencedor(notificaciones, PASADAS_24_H.minusSeconds(2), 50).vencerLote()).isZero();
        assertThat(vencedor(notificaciones, PASADAS_24_H, 50).vencerLote()).isEqualTo(1);
    }

    @Test
    @DisplayName("con un intento de pago en curso, la base respeta la gracia de 30 min (RF-55)")
    void graciaDelPago() {
        SolicitudDisponibilidad solicitud = confirmada();
        Instant finVentana = solicitud.venceEn();
        solicitud.marcarIntentoEnCurso(finVentana.minusSeconds(60));
        solicitud.extraerNotificaciones();
        guardar(solicitud);

        assertThat(vencedor(notificaciones, finVentana.plusSeconds(60), 50).vencerLote()).isZero();
        assertThat(vencedor(notificaciones, finVentana.plus(Duration.ofMinutes(30)), 50).vencerLote()).isEqualTo(1);
        assertThat(solicitudes.buscar(solicitud.id()).orElseThrow().estado()).isEqualTo(EstadoSolicitud.VENCIDA);
        assertThat(jdbc.queryForList("SELECT tipo FROM notificaciones_solicitud", String.class))
                .containsExactlyInAnyOrder("SOLICITUD_VENCIDA", "CUPO_LIBERADO");
    }

    @Test
    @DisplayName("el operador lee justo antes de las 24 h y el vencimiento gana: al reintentar ve Sin respuesta, 409")
    void atenderContraVencer() {
        SolicitudDisponibilidad solicitud = pendiente(MIERCOLES);
        guardar(solicitud);
        VencerSolicitudesService vencedor = vencedor(notificaciones, PASADAS_24_H, 50);
        // La primera lectura del operador deja que el vencimiento corra y confirme en otra transacción.
        SolicitudRepositoryPort conCruce = new LecturaQueCruza(solicitudes, vencedor);
        AtenderSolicitudService operador = new AtenderSolicitudService(conCruce, notificaciones, transaccion,
                new EjecutorConReintento(3, Duration.ZERO),
                Clock.fixed(AHORA.plus(Duration.ofHours(23)).plus(Duration.ofMinutes(59)), ZoneOffset.UTC));

        assertThatThrownBy(() -> operador.atender(new AtenderSolicitudCommand(OPERADOR, solicitud.id(),
                ResultadoAtencion.ATENDIDA, 300_000L, "Sin reembolso", true, null)))
                .isInstanceOf(TransicionInvalidaException.class)
                .hasMessageContaining("SIN_RESPUESTA");

        assertThat(solicitudes.buscar(solicitud.id()).orElseThrow().estado()).isEqualTo(EstadoSolicitud.SIN_RESPUESTA);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM notificaciones_solicitud WHERE tipo = "
                + "'SOLICITUD_ATENDIDA'", Integer.class)).as("la atención perdedora no dejó notificaciones").isZero();
    }

    private static SolicitudDisponibilidad pendiente(LocalDate fecha) {
        return SolicitudDisponibilidad.crear(UUID.randomUUID(), TURISTA, caminata(), fecha,
                CamposDelTipo.actividad(SIETE), 2, null, OPERADOR, AHORA);
    }

    private void guardar(SolicitudDisponibilidad solicitud) {
        transaccion.ejecutar(() -> {
            solicitudes.insertar(solicitud);
            notificaciones.registrar(solicitud.extraerNotificaciones());
            return null;
        });
    }

    private VencerSolicitudesService vencedor(NotificacionRepositoryPort destino, Instant ahora, int lote) {
        return new VencerSolicitudesService(solicitudes, destino, transaccion, Clock.fixed(ahora, ZoneOffset.UTC),
                lote);
    }

    private static int esperarYVencer(CountDownLatch salida, VencerSolicitudesService replica) {
        try {
            salida.await(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return replica.vencerLote();
    }

    /** Las notificaciones, con una demora que mantiene abierta la transacción del vencimiento. */
    private record NotificacionesLentas(NotificacionRepositoryPort destino, Duration demora)
            implements NotificacionRepositoryPort {

        @Override
        public void registrar(List<NotificacionSolicitud> nuevas) {
            try {
                Thread.sleep(demora.toMillis());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            destino.registrar(nuevas);
        }

        @Override
        public List<NotificacionPendiente> tomarPendientes(Instant ahora, int limite) {
            return destino.tomarPendientes(ahora, limite);
        }

        @Override
        public void guardar(NotificacionPendiente notificacion) {
            destino.guardar(notificacion);
        }
    }

    /**
     * El repositorio real, salvo que la primera lectura hace correr el vencimiento en otra transacción antes de
     * devolver la solicitud: el operador queda con una copia vieja, como si los dos hubieran llegado a la vez.
     */
    private static final class LecturaQueCruza implements SolicitudRepositoryPort {

        private final SolicitudRepositoryPort real;
        private final VencerSolicitudesService vencedor;
        private boolean primera = true;

        LecturaQueCruza(SolicitudRepositoryPort real, VencerSolicitudesService vencedor) {
            this.real = real;
            this.vencedor = vencedor;
        }

        @Override
        public Optional<SolicitudDisponibilidad> buscar(UUID id) {
            Optional<SolicitudDisponibilidad> leida = real.buscar(id);
            if (primera) {
                primera = false;
                assertThat(CompletableFuture.supplyAsync(vencedor::vencerLote).join()).isEqualTo(1);
            }
            return leida;
        }

        @Override
        public void insertar(SolicitudDisponibilidad solicitud) {
            real.insertar(solicitud);
        }

        @Override
        public void actualizar(SolicitudDisponibilidad solicitud) {
            real.actualizar(solicitud);
        }

        @Override
        public List<SolicitudDisponibilidad> abiertasDelTurista(UUID turistaId, String ofertaId, LocalDate fecha) {
            return real.abiertasDelTurista(turistaId, ofertaId, fecha);
        }

        @Override
        public Pagina<SolicitudDisponibilidad> delTurista(UUID turistaId, Paginacion p) {
            return real.delTurista(turistaId, p);
        }

        @Override
        public Pagina<SolicitudDisponibilidad> delOperador(UUID operadorId, EstadoSolicitud estado, Paginacion p) {
            return real.delOperador(operadorId, estado, p);
        }

        @Override
        public List<SolicitudDisponibilidad> tomarPorVencer(Instant ahora, int limite) {
            return real.tomarPorVencer(ahora, limite);
        }
    }
}
