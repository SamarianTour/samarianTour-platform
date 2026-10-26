package co.edu.unimagdalena.samariantour.nucleo.solicitudes;

import co.edu.unimagdalena.samariantour.nucleo.IntegracionPostgresBase;
import co.edu.unimagdalena.samariantour.nucleo.compartido.concurrencia.ConflictoConcurrenciaException;
import co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.excepcion.SolicitudDuplicadaException;
import co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.modelo.CamposDelTipo;
import co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.modelo.EstadoSolicitud;
import co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.modelo.Pagina;
import co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.modelo.Paginacion;
import co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.modelo.SolicitudDisponibilidad;
import co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.modelo.TransicionSolicitud;
import co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.puerto.salida.SolicitudRepositoryPort;
import co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.puerto.salida.TransaccionPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static co.edu.unimagdalena.samariantour.nucleo.solicitudes.SolicitudesDePrueba.AHORA;
import static co.edu.unimagdalena.samariantour.nucleo.solicitudes.SolicitudesDePrueba.MIERCOLES;
import static co.edu.unimagdalena.samariantour.nucleo.solicitudes.SolicitudesDePrueba.OPERADOR;
import static co.edu.unimagdalena.samariantour.nucleo.solicitudes.SolicitudesDePrueba.OTRO_OPERADOR;
import static co.edu.unimagdalena.samariantour.nucleo.solicitudes.SolicitudesDePrueba.SIETE;
import static co.edu.unimagdalena.samariantour.nucleo.solicitudes.SolicitudesDePrueba.TURISTA;
import static co.edu.unimagdalena.samariantour.nucleo.solicitudes.SolicitudesDePrueba.cabana;
import static co.edu.unimagdalena.samariantour.nucleo.solicitudes.SolicitudesDePrueba.caminata;
import static co.edu.unimagdalena.samariantour.nucleo.solicitudes.SolicitudesDePrueba.cocinaCaribe;
import static co.edu.unimagdalena.samariantour.nucleo.solicitudes.SolicitudesDePrueba.condiciones;
import static co.edu.unimagdalena.samariantour.nucleo.solicitudes.SolicitudesDePrueba.dosNoches;
import static co.edu.unimagdalena.samariantour.nucleo.solicitudes.SolicitudesDePrueba.pendiente;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Las solicitudes en PostgreSQL (DN-11): historial, índice de no duplicado, bloqueo optimista y SKIP LOCKED. */
@SpringBootTest(properties = {"nucleo.bandeja.publicacion.cron=-", "nucleo.solicitudes.vencimiento.cron=-",
        "nucleo.solicitudes.notificaciones.cron=-"})
class SolicitudesPersistenciaIT extends IntegracionPostgresBase {

    @Autowired
    private SolicitudRepositoryPort repositorio;

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
    @DisplayName("guarda la solicitud y la lee igual, con su historial en orden y la revisión de la base")
    void idaYVuelta() {
        SolicitudDisponibilidad solicitud = pendiente();
        insertar(solicitud);
        SolicitudDisponibilidad leida = buscar(solicitud.id());
        leida.atender(OPERADOR, condiciones(), AHORA.plus(Duration.ofHours(1)));
        transaccion.ejecutar(() -> {
            repositorio.actualizar(leida);
            return null;
        });

        SolicitudDisponibilidad otraVez = buscar(solicitud.id());

        assertThat(otraVez.estado()).isEqualTo(EstadoSolicitud.ATENDIDA);
        assertThat(otraVez.oferta()).isEqualTo(solicitud.oferta());
        assertThat(otraVez.fechaInicio()).isEqualTo(MIERCOLES);
        assertThat(otraVez.horaSalida()).isEqualTo(SIETE);
        assertThat(otraVez.notaTurista()).isEqualTo("Vamos con una niña de 8 años");
        assertThat(otraVez.condiciones()).isEqualTo(condiciones());
        assertThat(otraVez.venceEn()).isEqualTo(AHORA.plus(Duration.ofHours(4)));
        assertThat(otraVez.aparteHasta()).isEqualTo(AHORA.plus(Duration.ofHours(4)));
        assertThat(otraVez.creadaEn()).isEqualTo(solicitud.creadaEn());
        assertThat(otraVez.historial()).extracting(TransicionSolicitud::a)
                .containsExactly(EstadoSolicitud.PENDIENTE, EstadoSolicitud.ATENDIDA);
        assertThat(otraVez.historial()).isEqualTo(leida.historial());
        assertThat(otraVez.revision()).isGreaterThan(leida.revision());
        assertThat(jdbc.queryForObject("SELECT limite_vencimiento FROM solicitudes", Instant.class))
                .isEqualTo(AHORA.plus(Duration.ofHours(4)));
    }

    @Test
    @DisplayName("índice de no duplicado: otra abierta igual se rechaza; cerrada la primera, se puede pedir de nuevo")
    void noDuplicado() {
        SolicitudDisponibilidad primera = pendiente();
        insertar(primera);

        assertThatThrownBy(() -> insertar(pendiente())).isInstanceOf(SolicitudDuplicadaException.class)
                .extracting(e -> ((SolicitudDuplicadaException) e).codigo()).isEqualTo("SOLICITUD_DUPLICADA");

        SolicitudDisponibilidad cerrada = buscar(primera.id());
        cerrada.marcarNoDisponible(OPERADOR, null, AHORA.plusSeconds(60));
        transaccion.ejecutar(() -> {
            repositorio.actualizar(cerrada);
            return null;
        });
        insertar(pendiente());

        assertThat(jdbc.queryForObject("SELECT count(*) FROM solicitudes", Integer.class)).isEqualTo(2);
    }

    @Test
    @DisplayName("guarda los campos de alojamiento y de restaurante (V4)")
    void camposPorTipo() {
        SolicitudDisponibilidad cabana = SolicitudDisponibilidad.crear(UUID.randomUUID(), TURISTA, cabana(),
                MIERCOLES, CamposDelTipo.alojamiento(MIERCOLES.plusDays(3), 2), 4, null, OPERADOR, AHORA);
        SolicitudDisponibilidad cena = SolicitudDisponibilidad.crear(UUID.randomUUID(), TURISTA, cocinaCaribe(),
                MIERCOLES, CamposDelTipo.restaurante(LocalTime.of(19, 30)), 4, null, OPERADOR, AHORA);
        insertar(cabana);
        insertar(cena);

        assertThat(buscar(cabana.id()).campos()).isEqualTo(CamposDelTipo.alojamiento(MIERCOLES.plusDays(3), 2));
        assertThat(buscar(cena.id()).campos()).isEqualTo(CamposDelTipo.restaurante(LocalTime.of(19, 30)));
    }

    @Test
    @DisplayName("alojamiento: sin hora, se repite con la misma fecha de inicio aunque cambie la salida")
    void noDuplicadoAlojamiento() {
        insertar(SolicitudDisponibilidad.crear(UUID.randomUUID(), TURISTA, cabana(), MIERCOLES, dosNoches(MIERCOLES),
                2, null, OPERADOR, AHORA));

        assertThatThrownBy(() -> insertar(SolicitudDisponibilidad.crear(UUID.randomUUID(), TURISTA, cabana(),
                MIERCOLES, CamposDelTipo.alojamiento(MIERCOLES.plusDays(5), 1), 3, null, OPERADOR, AHORA)))
                .isInstanceOf(SolicitudDuplicadaException.class);
        insertar(SolicitudDisponibilidad.crear(UUID.randomUUID(), TURISTA, cabana(), MIERCOLES.plusDays(1),
                dosNoches(MIERCOLES.plusDays(1)), 2, null, OPERADOR, AHORA));
    }

    @Test
    @DisplayName("restaurante: se repite con la misma hora de la reserva; con otra hora, no")
    void noDuplicadoRestaurante() {
        insertar(SolicitudDisponibilidad.crear(UUID.randomUUID(), TURISTA, cocinaCaribe(), MIERCOLES,
                CamposDelTipo.restaurante(LocalTime.of(19, 30)), 4, null, OPERADOR, AHORA));

        assertThatThrownBy(() -> insertar(SolicitudDisponibilidad.crear(UUID.randomUUID(), TURISTA, cocinaCaribe(),
                MIERCOLES, CamposDelTipo.restaurante(LocalTime.of(19, 30)), 2, null, OPERADOR, AHORA)))
                .isInstanceOf(SolicitudDuplicadaException.class);
        insertar(SolicitudDisponibilidad.crear(UUID.randomUUID(), TURISTA, cocinaCaribe(), MIERCOLES,
                CamposDelTipo.restaurante(LocalTime.of(13, 0)), 4, null, OPERADOR, AHORA));
        assertThat(jdbc.queryForObject("SELECT count(*) FROM solicitudes", Integer.class)).isEqualTo(2);
    }

    @Test
    @DisplayName("dos envíos iguales a la vez, desde dos hilos: el índice deja pasar uno solo")
    void noDuplicadoALaVez() throws Exception {
        CountDownLatch insertoLaPrimera = new CountDownLatch(1);
        CompletableFuture<Void> primera = CompletableFuture.runAsync(() -> transaccion.ejecutar(() -> {
            repositorio.insertar(pendiente());
            insertoLaPrimera.countDown();
            dormir(Duration.ofMillis(500));
            return null;
        }));
        insertoLaPrimera.await(5, TimeUnit.SECONDS);

        // La segunda espera en el índice a que la primera confirme, y entonces choca.
        assertThatThrownBy(() -> insertar(pendiente())).isInstanceOf(SolicitudDuplicadaException.class);
        primera.get(5, TimeUnit.SECONDS);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM solicitudes", Integer.class)).isEqualTo(1);
    }

    @Test
    @DisplayName("bloqueo optimista: dos lecturas de la misma revisión, la segunda en guardar pierde y no guarda nada")
    void bloqueoOptimista() {
        SolicitudDisponibilidad solicitud = pendiente();
        insertar(solicitud);
        SolicitudDisponibilidad delOperador = buscar(solicitud.id());
        SolicitudDisponibilidad delVencimiento = buscar(solicitud.id());

        delOperador.atender(OPERADOR, condiciones(), AHORA.plus(Duration.ofHours(1)));
        transaccion.ejecutar(() -> {
            repositorio.actualizar(delOperador);
            return null;
        });
        delVencimiento.vencer(AHORA.plus(Duration.ofHours(24)));

        assertThatThrownBy(() -> transaccion.ejecutar(() -> {
            repositorio.actualizar(delVencimiento);
            return null;
        })).isInstanceOf(ConflictoConcurrenciaException.class);
        assertThat(buscar(solicitud.id()).estado()).isEqualTo(EstadoSolicitud.ATENDIDA);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM transiciones_solicitud", Integer.class)).isEqualTo(2);
    }

    @Test
    @DisplayName("la cola del operador: sus pendientes de la más antigua a la más reciente, paginada")
    void colaDelOperador() {
        SolicitudDisponibilidad primera = pendiente(AHORA);
        SolicitudDisponibilidad segunda = SolicitudDisponibilidad.crear(UUID.randomUUID(), TURISTA, caminata(),
                MIERCOLES, CamposDelTipo.actividad(LocalTime.of(8, 0)), 2, null, OPERADOR, AHORA.plusSeconds(60));
        SolicitudDisponibilidad deOtro = SolicitudDisponibilidad.crear(UUID.randomUUID(), TURISTA, caminata(),
                MIERCOLES.plusDays(1), CamposDelTipo.actividad(SIETE), 2, null, OTRO_OPERADOR,
                AHORA.plusSeconds(120));
        insertar(segunda);
        insertar(primera);
        insertar(deOtro);

        Pagina<SolicitudDisponibilidad> cola = repositorio.delOperador(OPERADOR, EstadoSolicitud.PENDIENTE,
                new Paginacion(0, 1));

        assertThat(cola.contenido()).extracting(SolicitudDisponibilidad::id).containsExactly(primera.id());
        assertThat(cola.totalElementos()).isEqualTo(2);
        assertThat(cola.totalPaginas()).isEqualTo(2);
        assertThat(repositorio.delTurista(TURISTA, new Paginacion(0, 10)).contenido())
                .extracting(SolicitudDisponibilidad::id)
                .as("las del turista, de la más reciente a la más antigua")
                .containsExactly(deOtro.id(), segunda.id(), primera.id());
    }

    @Test
    @DisplayName("tomar por vencer: solo las abiertas que ya tocan; dos transacciones a la vez no toman la misma")
    void tomarPorVencerSaltaBloqueadas() throws Exception {
        for (int i = 0; i < 4; i++) {
            insertar(SolicitudDisponibilidad.crear(UUID.randomUUID(), TURISTA, caminata(), MIERCOLES.plusDays(i),
                    CamposDelTipo.actividad(SIETE), 2, null, OPERADOR, AHORA));
        }
        insertar(SolicitudDisponibilidad.crear(UUID.randomUUID(), TURISTA, caminata(), MIERCOLES.plusDays(9),
                CamposDelTipo.actividad(SIETE), 2, null, OPERADOR, AHORA.plus(Duration.ofHours(2))));
        Instant ahora = AHORA.plus(Duration.ofHours(24)).plusSeconds(1);

        CountDownLatch tomoLaPrimera = new CountDownLatch(1);
        CompletableFuture<List<UUID>> primera = CompletableFuture.supplyAsync(() -> transaccion.ejecutar(() -> {
            List<UUID> tomadas = repositorio.tomarPorVencer(ahora, 2).stream()
                    .map(SolicitudDisponibilidad::id).toList();
            tomoLaPrimera.countDown();
            dormir(Duration.ofMillis(500));
            return tomadas;
        }));
        tomoLaPrimera.await(5, TimeUnit.SECONDS);
        List<UUID> segunda = transaccion.ejecutar(() -> repositorio.tomarPorVencer(ahora, 10).stream()
                .map(SolicitudDisponibilidad::id).toList());

        assertThat(primera.get(5, TimeUnit.SECONDS)).hasSize(2);
        assertThat(segunda).as("las otras dos que ya vencieron; no la que vence más tarde").hasSize(2)
                .doesNotContainAnyElementsOf(primera.get());
    }

    private void insertar(SolicitudDisponibilidad solicitud) {
        transaccion.ejecutar(() -> {
            repositorio.insertar(solicitud);
            return null;
        });
    }

    private SolicitudDisponibilidad buscar(UUID id) {
        return repositorio.buscar(id).orElseThrow();
    }

    private static void dormir(Duration duracion) {
        try {
            Thread.sleep(duracion.toMillis());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
