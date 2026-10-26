package co.edu.unimagdalena.samariantour.nucleo.solicitudes;

import co.edu.unimagdalena.samariantour.nucleo.IntegracionPostgresBase;
import co.edu.unimagdalena.samariantour.nucleo.compartido.concurrencia.EjecutorConReintento;
import co.edu.unimagdalena.samariantour.nucleo.solicitudes.aplicacion.EjecucionIdempotente;
import co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.excepcion.ClaveIdempotenciaReutilizadaException;
import co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.modelo.CamposDelTipo;
import co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.modelo.PeticionIdempotente;
import co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.modelo.RespuestaGuardada;
import co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.modelo.ResultadoIdempotente;
import co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.modelo.SolicitudDisponibilidad;
import co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.puerto.salida.IdempotenciaPort;
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
import java.time.ZoneId;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static co.edu.unimagdalena.samariantour.nucleo.solicitudes.SolicitudesDePrueba.AHORA;
import static co.edu.unimagdalena.samariantour.nucleo.solicitudes.SolicitudesDePrueba.OTRO_TURISTA;
import static co.edu.unimagdalena.samariantour.nucleo.solicitudes.SolicitudesDePrueba.TURISTA;
import static co.edu.unimagdalena.samariantour.nucleo.solicitudes.SolicitudesDePrueba.MIERCOLES;
import static co.edu.unimagdalena.samariantour.nucleo.solicitudes.SolicitudesDePrueba.OPERADOR;
import static co.edu.unimagdalena.samariantour.nucleo.solicitudes.SolicitudesDePrueba.SIETE;
import static co.edu.unimagdalena.samariantour.nucleo.solicitudes.SolicitudesDePrueba.caminata;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Las claves de idempotencia en PostgreSQL (ADR-04, DN-12): cada caso cuenta cuántas solicitudes quedan creadas. */
@SpringBootTest(properties = {"nucleo.bandeja.publicacion.cron=-", "nucleo.solicitudes.vencimiento.cron=-",
        "nucleo.solicitudes.notificaciones.cron=-"})
class IdempotenciaIT extends IntegracionPostgresBase {

    private static final UUID CLAVE = UUID.fromString("c0000000-0000-0000-0000-00000000000a");

    @Autowired
    private IdempotenciaPort claves;

    @Autowired
    private TransaccionPort transaccion;

    @Autowired
    private SolicitudRepositoryPort solicitudes;

    @Autowired
    private JdbcTemplate jdbc;

    private final AtomicReference<Instant> ahora = new AtomicReference<>(AHORA);
    private final AtomicInteger ejecuciones = new AtomicInteger();
    private EjecucionIdempotente idempotente;

    @BeforeEach
    void preparar() {
        jdbc.update("DELETE FROM claves_idempotencia");
        jdbc.update("DELETE FROM notificaciones_solicitud");
        jdbc.update("DELETE FROM transiciones_solicitud");
        jdbc.update("DELETE FROM solicitudes");
        idempotente = new EjecucionIdempotente(claves, transaccion, new EjecutorConReintento(3, Duration.ZERO),
                new RelojMovil(ahora));
    }

    @Test
    @DisplayName("la misma clave con la misma petición: la segunda devuelve la respuesta original sin crear nada")
    void mismaClave() {
        ResultadoIdempotente primera = enviar(peticion(TURISTA, "{\"personas\":2}"), Duration.ZERO);
        ResultadoIdempotente segunda = enviar(peticion(TURISTA, "{\"personas\":2}"), Duration.ZERO);

        assertThat(primera.repetida()).isFalse();
        assertThat(segunda.repetida()).isTrue();
        assertThat(segunda.respuesta()).isEqualTo(primera.respuesta());
        assertThat(ejecuciones.get()).isEqualTo(1);
        assertThat(contar("solicitudes")).isEqualTo(1);
    }

    @Test
    @DisplayName("la misma clave a la vez desde dos hilos: la segunda espera a la primera y repite su respuesta")
    void aLaVez() throws Exception {
        CountDownLatch empezo = new CountDownLatch(1);
        CompletableFuture<ResultadoIdempotente> primera = CompletableFuture.supplyAsync(() ->
                idempotente.ejecutar(peticion(TURISTA, "{}"), () -> {
                    empezo.countDown();
                    return crearSolicitud(Duration.ofMillis(500));
                }));
        empezo.await(5, TimeUnit.SECONDS);

        ResultadoIdempotente segunda = enviar(peticion(TURISTA, "{}"), Duration.ZERO);

        assertThat(segunda.repetida()).isTrue();
        assertThat(segunda.respuesta()).isEqualTo(primera.get(5, TimeUnit.SECONDS).respuesta());
        assertThat(ejecuciones.get()).isEqualTo(1);
        assertThat(contar("solicitudes")).isEqualTo(1);
    }

    @Test
    @DisplayName("la misma clave con otra petición: 422 CLAVE_IDEMPOTENCIA_REUTILIZADA, sin hacer nada")
    void otraPeticion() {
        enviar(peticion(TURISTA, "{\"personas\":2}"), Duration.ZERO);

        assertThatThrownBy(() -> enviar(peticion(TURISTA, "{\"personas\":3}"), Duration.ZERO))
                .isInstanceOf(ClaveIdempotenciaReutilizadaException.class)
                .extracting(e -> ((ClaveIdempotenciaReutilizadaException) e).codigo())
                .isEqualTo("CLAVE_IDEMPOTENCIA_REUTILIZADA");
        assertThat(ejecuciones.get()).isEqualTo(1);
    }

    @Test
    @DisplayName("si la operación falla no queda nada: el reintento con la misma clave la hace (ADR-04)")
    void fallaYReintenta() {
        assertThatThrownBy(() -> idempotente.ejecutar(peticion(TURISTA, "{}"), () -> {
            crearSolicitud(Duration.ZERO);
            throw new IllegalStateException("el catálogo no respondió");
        })).isInstanceOf(IllegalStateException.class);
        assertThat(contar("claves_idempotencia")).isZero();
        assertThat(contar("solicitudes")).as("lo que alcanzó a hacer se deshizo").isZero();

        ResultadoIdempotente reintento = enviar(peticion(TURISTA, "{}"), Duration.ZERO);

        assertThat(reintento.repetida()).isFalse();
        assertThat(contar("solicitudes")).isEqualTo(1);
    }

    @Test
    @DisplayName("cada usuario tiene sus claves; una clave vencida (24 h) vuelve a contar como nueva")
    void porUsuarioYVigencia() {
        enviar(peticion(TURISTA, "{}"), Duration.ZERO);
        assertThat(enviar(peticion(OTRO_TURISTA, "{\"otro\":true}"), Duration.ZERO).repetida()).isFalse();

        ahora.set(AHORA.plus(Duration.ofHours(24)));
        assertThat(enviar(peticion(TURISTA, "{\"otra\":true}"), Duration.ZERO).repetida()).isFalse();
        assertThat(ejecuciones.get()).isEqualTo(3);

        assertThat(transaccion.ejecutar(() -> claves.purgarVencidas(AHORA.plus(Duration.ofHours(25))))).isEqualTo(1);
    }

    private ResultadoIdempotente enviar(PeticionIdempotente peticion, Duration demora) {
        return idempotente.ejecutar(peticion, () -> crearSolicitud(demora));
    }

    /** Cada ejecución crea una solicitud distinta (otra fecha), para que el índice de no duplicado no intervenga. */
    private RespuestaGuardada crearSolicitud(Duration demora) {
        int numero = ejecuciones.incrementAndGet();
        SolicitudDisponibilidad solicitud = SolicitudDisponibilidad.crear(UUID.randomUUID(), TURISTA, caminata(),
                MIERCOLES.plusDays(numero), CamposDelTipo.actividad(SIETE), 2, null, OPERADOR, AHORA);
        solicitudes.insertar(solicitud);
        dormir(demora);
        return new RespuestaGuardada(201, "{\"id\":\"%s\"}".formatted(solicitud.id()));
    }

    private static PeticionIdempotente peticion(UUID usuario, String cuerpo) {
        return PeticionIdempotente.de(CLAVE, usuario, "ENVIAR_SOLICITUD", cuerpo);
    }

    private int contar(String tabla) {
        return jdbc.queryForObject("SELECT count(*) FROM " + tabla, Integer.class);
    }

    private static void dormir(Duration duracion) {
        try {
            Thread.sleep(duracion.toMillis());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /** Un reloj que la prueba adelanta. */
    private static final class RelojMovil extends Clock {

        private final AtomicReference<Instant> ahora;

        RelojMovil(AtomicReference<Instant> ahora) {
            this.ahora = ahora;
        }

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
            return ahora.get();
        }
    }
}
