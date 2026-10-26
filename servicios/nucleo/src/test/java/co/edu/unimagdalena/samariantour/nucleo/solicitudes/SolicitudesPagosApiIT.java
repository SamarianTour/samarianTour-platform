package co.edu.unimagdalena.samariantour.nucleo.solicitudes;

import co.edu.unimagdalena.samariantour.nucleo.IntegracionPostgresBase;
import co.edu.unimagdalena.samariantour.nucleo.solicitudes.api.EventoPagoNoAplicableException;
import co.edu.unimagdalena.samariantour.nucleo.solicitudes.api.ResultadoPago;
import co.edu.unimagdalena.samariantour.nucleo.solicitudes.api.SolicitudParaCobro;
import co.edu.unimagdalena.samariantour.nucleo.solicitudes.api.SolicitudesPagosApi;
import co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.modelo.CamposDelTipo;
import co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.modelo.EstadoSolicitud;
import co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.modelo.SolicitudDisponibilidad;
import co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.puerto.salida.NotificacionRepositoryPort;
import co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.puerto.salida.SolicitudRepositoryPort;
import co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.puerto.salida.TransaccionPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.IllegalTransactionStateException;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

import static co.edu.unimagdalena.samariantour.nucleo.solicitudes.SolicitudesDePrueba.AHORA;
import static co.edu.unimagdalena.samariantour.nucleo.solicitudes.SolicitudesDePrueba.OTRO_TURISTA;
import static co.edu.unimagdalena.samariantour.nucleo.solicitudes.SolicitudesDePrueba.TURISTA;
import static co.edu.unimagdalena.samariantour.nucleo.solicitudes.SolicitudesDePrueba.confirmada;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * La API que usará el módulo de pagos (DN-17), probada como la llamará: dentro de una transacción que abre pagos. Hasta
 * que exista ese módulo, la transacción la abre la prueba.
 */
@SpringBootTest(properties = {"nucleo.bandeja.publicacion.cron=-", "nucleo.solicitudes.vencimiento.cron=-",
        "nucleo.solicitudes.notificaciones.cron=-"})
class SolicitudesPagosApiIT extends IntegracionPostgresBase {

    /** El reloj del servicio, que la prueba mueve: la ventana de las solicitudes de prueba vence a las 19:00 UTC. */
    static final AtomicReference<Instant> AHORA_DEL_SERVICIO = new AtomicReference<>(AHORA);

    @TestConfiguration
    static class RelojDePrueba {
        @Bean
        @Primary
        Clock relojDePrueba() {
            return new Clock() {
                @Override
                public ZoneId getZone() {
                    return ZoneId.of("America/Bogota");
                }

                @Override
                public Clock withZone(ZoneId zona) {
                    return this;
                }

                @Override
                public Instant instant() {
                    return AHORA_DEL_SERVICIO.get();
                }
            };
        }
    }

    @Autowired
    private SolicitudesPagosApi pagos;

    @Autowired
    private SolicitudRepositoryPort solicitudes;

    @Autowired
    private NotificacionRepositoryPort notificaciones;

    @Autowired
    private TransaccionPort transaccion;

    @Autowired
    private JdbcTemplate jdbc;

    private Instant finVentana;

    @BeforeEach
    void vaciar() {
        jdbc.update("DELETE FROM notificaciones_solicitud");
        jdbc.update("DELETE FROM transiciones_solicitud");
        jdbc.update("DELETE FROM solicitudes");
        finVentana = AHORA.plus(Duration.ofHours(4));
        AHORA_DEL_SERVICIO.set(AHORA.plus(Duration.ofHours(2)));
    }

    @Test
    @DisplayName("para cobrar: la confirmada del turista, con su precio y su ventana; otra persona o estado, vacío")
    void consultarParaCobro() {
        SolicitudDisponibilidad solicitud = guardar(confirmada());
        SolicitudDisponibilidad sinConfirmar = guardar(atendidaOtroDia());

        SolicitudParaCobro cobro = pagos.consultarParaCobro(solicitud.id(), TURISTA).orElseThrow();

        assertThat(cobro.estado()).isEqualTo("CONFIRMADA");
        assertThat(cobro.precioBase()).isEqualTo(300_000);
        assertThat(cobro.venceEn()).isEqualTo(finVentana);
        assertThat(cobro.ofertaId()).isEqualTo(solicitud.ofertaId());
        assertThat(pagos.consultarParaCobro(solicitud.id(), OTRO_TURISTA)).isEmpty();
        assertThat(pagos.consultarParaCobro(sinConfirmar.id(), TURISTA)).isEmpty();
    }

    @Test
    @DisplayName("intento en curso y pago aprobado en la transacción de pagos: Pagada, con sus notificaciones")
    void pagoAprobado() {
        SolicitudDisponibilidad solicitud = guardar(confirmada());

        UUID id = solicitud.id();
        assertThat(enTransaccionDePagos(() -> pagos.marcarIntentoEnCurso(id))).isEqualTo(ResultadoPago.APLICADO);
        assertThat(enTransaccionDePagos(() -> pagos.registrarPagoAprobado(id))).isEqualTo(ResultadoPago.APLICADO);
        assertThat(enTransaccionDePagos(() -> pagos.registrarPagoAprobado(id))).isEqualTo(ResultadoPago.SIN_CAMBIOS);

        assertThat(estado(solicitud)).isEqualTo(EstadoSolicitud.PAGADA);
        assertThat(jdbc.queryForList("SELECT tipo FROM notificaciones_solicitud", String.class))
                .containsExactly("SOLICITUD_PAGADA", "SOLICITUD_PAGADA");
    }

    @Test
    @DisplayName("si pagos deshace su transacción, la solicitud tampoco queda Pagada")
    void seDeshaceConPagos() {
        SolicitudDisponibilidad solicitud = guardar(confirmada());

        assertThatThrownBy(() -> enTransaccionDePagos(() -> {
            pagos.registrarPagoAprobado(solicitud.id());
            throw new IllegalStateException("falló el registro del pago");
        })).isInstanceOf(IllegalStateException.class);

        assertThat(estado(solicitud)).isEqualTo(EstadoSolicitud.CONFIRMADA);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM notificaciones_solicitud", Integer.class)).isZero();
    }

    @Test
    @DisplayName("tres rechazos: Pago fallido dos veces y Vencida al tercero (RF-58)")
    void tresRechazos() {
        SolicitudDisponibilidad solicitud = guardar(confirmada());

        for (int intento = 1; intento <= 3; intento++) {
            enTransaccionDePagos(() -> pagos.marcarIntentoEnCurso(solicitud.id()));
            enTransaccionDePagos(() -> pagos.registrarPagoRechazado(solicitud.id()));
        }

        assertThat(estado(solicitud)).isEqualTo(EstadoSolicitud.VENCIDA);
        assertThat(jdbc.queryForObject("SELECT intentos_pago_fallidos FROM solicitudes", Integer.class)).isEqualTo(3);
    }

    @Test
    @DisplayName("aprobado sobre una ya vencida: PAGO_TARDIO, no la revive (RF-50)")
    void pagoTardio() {
        SolicitudDisponibilidad solicitud = guardar(confirmada());
        AHORA_DEL_SERVICIO.set(finVentana.plusSeconds(30));

        assertThat(enTransaccionDePagos(() -> pagos.registrarPagoAprobado(solicitud.id())))
                .isEqualTo(ResultadoPago.PAGO_TARDIO);

        assertThat(estado(solicitud)).isEqualTo(EstadoSolicitud.VENCIDA);
    }

    @Test
    @DisplayName("sin la transacción de pagos no se aplica; un evento sobre una no confirmada es TRANSICION_INVALIDA")
    void exigeTransaccionYEstado() {
        SolicitudDisponibilidad solicitud = guardar(confirmada());
        SolicitudDisponibilidad sinConfirmar = guardar(atendidaOtroDia());

        assertThatThrownBy(() -> pagos.registrarPagoAprobado(solicitud.id()))
                .isInstanceOf(IllegalTransactionStateException.class);
        assertThatThrownBy(() -> enTransaccionDePagos(() -> pagos.registrarPagoAprobado(sinConfirmar.id())))
                .isInstanceOf(EventoPagoNoAplicableException.class)
                .extracting(e -> ((EventoPagoNoAplicableException) e).codigo()).isEqualTo("TRANSICION_INVALIDA");
        assertThatThrownBy(() -> enTransaccionDePagos(() -> pagos.registrarPagoAprobado(UUID.randomUUID())))
                .isInstanceOf(EventoPagoNoAplicableException.class)
                .extracting(e -> ((EventoPagoNoAplicableException) e).codigo()).isEqualTo("SOLICITUD_NO_ENCONTRADA");
    }

    private static SolicitudDisponibilidad atendidaOtroDia() {
        SolicitudDisponibilidad solicitud = SolicitudDisponibilidad.crear(UUID.randomUUID(), TURISTA,
                SolicitudesDePrueba.caminata(), SolicitudesDePrueba.MIERCOLES.plusDays(1),
                CamposDelTipo.actividad(SolicitudesDePrueba.SIETE), 2, null, SolicitudesDePrueba.OPERADOR, AHORA);
        solicitud.atender(SolicitudesDePrueba.OPERADOR, SolicitudesDePrueba.condiciones(),
                AHORA.plus(Duration.ofHours(1)));
        solicitud.extraerNotificaciones();
        return solicitud;
    }

    private SolicitudDisponibilidad guardar(SolicitudDisponibilidad solicitud) {
        return transaccion.ejecutar(() -> {
            solicitudes.insertar(solicitud);
            notificaciones.registrar(solicitud.extraerNotificaciones());
            return solicitud;
        });
    }

    private <T> T enTransaccionDePagos(Supplier<T> llamada) {
        return transaccion.ejecutar(llamada);
    }

    private EstadoSolicitud estado(SolicitudDisponibilidad solicitud) {
        return solicitudes.buscar(solicitud.id()).orElseThrow().estado();
    }
}
