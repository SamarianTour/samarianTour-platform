package co.edu.unimagdalena.samariantour.nucleo.solicitudes;

import co.edu.unimagdalena.samariantour.nucleo.IntegracionPostgresBase;
import co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.modelo.CamposDelTipo;
import co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.modelo.CategoriaPrestador;
import co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.modelo.ConteoCategoria;
import co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.modelo.ConteoDemanda;
import co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.modelo.ConteoEstado;
import co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.modelo.DecisionTurista;
import co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.modelo.EstadoSolicitud;
import co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.modelo.SolicitudDisponibilidad;
import co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.modelo.TipoOferta;
import co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.puerto.entrada.ContarSolicitudesUseCase;
import co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.puerto.salida.SolicitudRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import static co.edu.unimagdalena.samariantour.nucleo.solicitudes.SolicitudesDePrueba.AHORA;
import static co.edu.unimagdalena.samariantour.nucleo.solicitudes.SolicitudesDePrueba.MIERCOLES;
import static co.edu.unimagdalena.samariantour.nucleo.solicitudes.SolicitudesDePrueba.OPERADOR;
import static co.edu.unimagdalena.samariantour.nucleo.solicitudes.SolicitudesDePrueba.SIETE;
import static co.edu.unimagdalena.samariantour.nucleo.solicitudes.SolicitudesDePrueba.TAYRONA;
import static co.edu.unimagdalena.samariantour.nucleo.solicitudes.SolicitudesDePrueba.TURISTA;
import static co.edu.unimagdalena.samariantour.nucleo.solicitudes.SolicitudesDePrueba.cabana;
import static co.edu.unimagdalena.samariantour.nucleo.solicitudes.SolicitudesDePrueba.caminata;
import static co.edu.unimagdalena.samariantour.nucleo.solicitudes.SolicitudesDePrueba.condiciones;
import static co.edu.unimagdalena.samariantour.nucleo.solicitudes.SolicitudesDePrueba.dosNoches;
import static org.assertj.core.api.Assertions.assertThat;

/** Los conteos de los indicadores en PostgreSQL (RF-85 a RF-89, DN-41): el periodo, el historial y la categoría. */
@SpringBootTest(properties = "nucleo.bandeja.publicacion.cron=-")
class ConteosSolicitudesIT extends IntegracionPostgresBase {

    private static final Instant DESDE = AHORA.minus(Duration.ofDays(1));
    private static final Instant HASTA = AHORA.plus(Duration.ofDays(1));

    @Autowired
    private SolicitudRepositoryPort repositorio;

    @Autowired
    private ContarSolicitudesUseCase conteos;

    @Autowired
    private JdbcTemplate jdbc;

    @BeforeEach
    void vaciar() {
        jdbc.update("DELETE FROM notificaciones_solicitud");
        jdbc.update("DELETE FROM transiciones_solicitud");
        jdbc.update("DELETE FROM solicitudes");
    }

    @Test
    @DisplayName("cuenta por zona y tipo solo las enviadas en el periodo")
    void porZonaYTipo() {
        guardar(caminataEnviada(AHORA, CategoriaPrestador.MICRO_PEQUENO));
        guardar(caminataEnviada(AHORA.plusSeconds(60), CategoriaPrestador.MICRO_PEQUENO));
        guardar(cabanaEnviada());
        guardar(caminataEnviada(HASTA, CategoriaPrestador.MICRO_PEQUENO));

        assertThat(conteos.porZonaYTipo(DESDE, HASTA)).containsExactlyInAnyOrder(
                new ConteoDemanda(TAYRONA, TipoOferta.ACTIVIDAD, 2),
                new ConteoDemanda(cabana().datos().zonaId(), TipoOferta.ALOJAMIENTO, 1));
    }

    @Test
    @DisplayName("el embudo cuenta los estados por los que pasó cada una, aunque después haya cambiado")
    void porEstadoAlcanzado() {
        SolicitudDisponibilidad pagada = caminataEnviada(AHORA, CategoriaPrestador.MICRO_PEQUENO);
        pagada.atender(OPERADOR, condiciones(), AHORA.plus(Duration.ofHours(1)));
        pagada.responder(TURISTA, DecisionTurista.CONFIRMAR, AHORA.plus(Duration.ofHours(2)));
        pagada.registrarPagoAprobado(AHORA.plus(Duration.ofHours(3)));
        SolicitudDisponibilidad declinada = caminataEnviada(AHORA.plusSeconds(60), CategoriaPrestador.MICRO_PEQUENO);
        declinada.atender(OPERADOR, condiciones(), AHORA.plus(Duration.ofHours(1)));
        declinada.responder(TURISTA, DecisionTurista.DECLINAR, AHORA.plus(Duration.ofHours(2)));
        guardar(pagada);
        guardar(declinada);

        assertThat(conteos.porEstadoAlcanzado(DESDE, HASTA)).containsExactlyInAnyOrder(
                new ConteoEstado(TipoOferta.ACTIVIDAD, EstadoSolicitud.PENDIENTE, 2),
                new ConteoEstado(TipoOferta.ACTIVIDAD, EstadoSolicitud.ATENDIDA, 2),
                new ConteoEstado(TipoOferta.ACTIVIDAD, EstadoSolicitud.CONFIRMADA, 1),
                new ConteoEstado(TipoOferta.ACTIVIDAD, EstadoSolicitud.PAGADA, 1),
                new ConteoEstado(TipoOferta.ACTIVIDAD, EstadoSolicitud.DECLINADA, 1));
    }

    @Test
    @DisplayName("por categoría, con las pagadas; la categoría guardada no cambia con el prestador")
    void porCategoria() {
        SolicitudDisponibilidad pagada = caminataEnviada(AHORA, CategoriaPrestador.MEDIANO);
        pagada.atender(OPERADOR, condiciones(), AHORA.plus(Duration.ofHours(1)));
        pagada.responder(TURISTA, DecisionTurista.CONFIRMAR, AHORA.plus(Duration.ofHours(2)));
        pagada.registrarPagoAprobado(AHORA.plus(Duration.ofHours(3)));
        guardar(pagada);
        guardar(caminataEnviada(AHORA.plusSeconds(60), CategoriaPrestador.MICRO_PEQUENO));
        guardar(cabanaEnviada());

        assertThat(conteos.porCategoria(DESDE, HASTA)).containsExactlyInAnyOrder(
                new ConteoCategoria(CategoriaPrestador.MEDIANO, 1, 1),
                new ConteoCategoria(CategoriaPrestador.MICRO_PEQUENO, 1, 0),
                new ConteoCategoria(null, 1, 0));
        assertThat(repositorio.buscar(pagada.id())).hasValueSatisfying(guardada ->
                assertThat(guardada.categoriaPrestador()).isEqualTo(CategoriaPrestador.MEDIANO));
    }

    private static SolicitudDisponibilidad caminataEnviada(Instant enviadaEn, CategoriaPrestador categoria) {
        return SolicitudDisponibilidad.crear(UUID.randomUUID(), UUID.randomUUID(), caminata(), MIERCOLES,
                CamposDelTipo.actividad(SIETE), 2, null, OPERADOR, categoria, enviadaEn);
    }

    /** Sin categoría, como una solicitud anterior a V14 cuyo prestador no se encontró. */
    private static SolicitudDisponibilidad cabanaEnviada() {
        return SolicitudDisponibilidad.crear(UUID.randomUUID(), UUID.randomUUID(), cabana(), MIERCOLES,
                dosNoches(MIERCOLES), 2, null, OPERADOR, AHORA);
    }

    private void guardar(SolicitudDisponibilidad solicitud) {
        solicitud.extraerNotificaciones();
        repositorio.insertar(solicitud);
    }
}
