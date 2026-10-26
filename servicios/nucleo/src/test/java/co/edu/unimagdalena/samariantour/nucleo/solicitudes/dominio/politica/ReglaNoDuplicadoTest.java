package co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.politica;

import co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.excepcion.SolicitudDuplicadaException;
import co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.modelo.CamposDelTipo;
import co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.modelo.DecisionTurista;
import co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.modelo.SolicitudDisponibilidad;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

import static co.edu.unimagdalena.samariantour.nucleo.solicitudes.SolicitudesDePrueba.AHORA;
import static co.edu.unimagdalena.samariantour.nucleo.solicitudes.SolicitudesDePrueba.CABANA;
import static co.edu.unimagdalena.samariantour.nucleo.solicitudes.SolicitudesDePrueba.CABO_SAN_JUAN;
import static co.edu.unimagdalena.samariantour.nucleo.solicitudes.SolicitudesDePrueba.COCINA_CARIBE;
import static co.edu.unimagdalena.samariantour.nucleo.solicitudes.SolicitudesDePrueba.MIERCOLES;
import static co.edu.unimagdalena.samariantour.nucleo.solicitudes.SolicitudesDePrueba.OPERADOR;
import static co.edu.unimagdalena.samariantour.nucleo.solicitudes.SolicitudesDePrueba.OTRO_TURISTA;
import static co.edu.unimagdalena.samariantour.nucleo.solicitudes.SolicitudesDePrueba.SIETE;
import static co.edu.unimagdalena.samariantour.nucleo.solicitudes.SolicitudesDePrueba.TURISTA;
import static co.edu.unimagdalena.samariantour.nucleo.solicitudes.SolicitudesDePrueba.atendida;
import static co.edu.unimagdalena.samariantour.nucleo.solicitudes.SolicitudesDePrueba.cabana;
import static co.edu.unimagdalena.samariantour.nucleo.solicitudes.SolicitudesDePrueba.cocinaCaribe;
import static co.edu.unimagdalena.samariantour.nucleo.solicitudes.SolicitudesDePrueba.dosNoches;
import static co.edu.unimagdalena.samariantour.nucleo.solicitudes.SolicitudesDePrueba.pendiente;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ReglaNoDuplicadoTest {

    @Test
    @DisplayName("misma oferta, fecha y hora con una no terminal del mismo turista: duplicada, con su identificador")
    void duplicada() {
        SolicitudDisponibilidad existente = pendiente();

        assertThatThrownBy(() -> ReglaNoDuplicado.verificar(TURISTA, CABO_SAN_JUAN, MIERCOLES, SIETE,
                List.of(existente)))
                .isInstanceOf(SolicitudDuplicadaException.class)
                .hasMessageContaining(existente.id().toString());
    }

    @Test
    @DisplayName("también si la abierta ya está atendida o confirmada")
    void abiertaAvanzada() {
        assertThatThrownBy(() -> ReglaNoDuplicado.verificar(TURISTA, CABO_SAN_JUAN, MIERCOLES, SIETE,
                List.of(atendida()))).isInstanceOf(SolicitudDuplicadaException.class);
    }

    @Test
    @DisplayName("otra hora, otra fecha, otra oferta u otro turista no chocan")
    void distintas() {
        List<SolicitudDisponibilidad> existentes = List.of(pendiente());

        assertThatCode(() -> ReglaNoDuplicado.verificar(TURISTA, CABO_SAN_JUAN, MIERCOLES, LocalTime.of(8, 0),
                existentes)).doesNotThrowAnyException();
        assertThatCode(() -> ReglaNoDuplicado.verificar(TURISTA, CABO_SAN_JUAN, MIERCOLES.plusDays(1), SIETE,
                existentes)).doesNotThrowAnyException();
        assertThatCode(() -> ReglaNoDuplicado.verificar(TURISTA, CABANA, MIERCOLES, null, existentes))
                .doesNotThrowAnyException();
        assertThatCode(() -> ReglaNoDuplicado.verificar(OTRO_TURISTA, CABO_SAN_JUAN, MIERCOLES, SIETE, existentes))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("restaurante: compara la hora de la reserva; alojamiento: solo la fecha de inicio")
    void horaDeCadaTipo() {
        SolicitudDisponibilidad cena = SolicitudDisponibilidad.crear(UUID.randomUUID(), TURISTA, cocinaCaribe(),
                MIERCOLES, CamposDelTipo.restaurante(LocalTime.of(19, 30)), 4, null, OPERADOR, AHORA);
        SolicitudDisponibilidad cabana = SolicitudDisponibilidad.crear(UUID.randomUUID(), TURISTA, cabana(),
                MIERCOLES, dosNoches(MIERCOLES), 2, null, OPERADOR, AHORA);
        List<SolicitudDisponibilidad> existentes = List.of(cena, cabana);

        assertThatThrownBy(() -> ReglaNoDuplicado.verificar(TURISTA, COCINA_CARIBE, MIERCOLES, LocalTime.of(19, 30),
                existentes)).isInstanceOf(SolicitudDuplicadaException.class);
        assertThatCode(() -> ReglaNoDuplicado.verificar(TURISTA, COCINA_CARIBE, MIERCOLES, LocalTime.of(13, 0),
                existentes)).doesNotThrowAnyException();
        assertThatThrownBy(() -> ReglaNoDuplicado.verificar(TURISTA, CABANA, MIERCOLES, null, existentes))
                .isInstanceOf(SolicitudDuplicadaException.class).hasMessageContaining(cabana.id().toString());
        assertThatCode(() -> ReglaNoDuplicado.verificar(TURISTA, CABANA, MIERCOLES.plusDays(1), null, existentes))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("una terminal (declinada, sin respuesta) ya no impide pedir lo mismo otra vez")
    void terminales() {
        SolicitudDisponibilidad declinada = atendida();
        declinada.responder(TURISTA, DecisionTurista.DECLINAR, AHORA.plus(Duration.ofHours(2)));
        SolicitudDisponibilidad sinRespuesta = pendiente();
        sinRespuesta.vencer(AHORA.plus(Duration.ofHours(24)));

        assertThatCode(() -> ReglaNoDuplicado.verificar(TURISTA, CABO_SAN_JUAN, MIERCOLES, SIETE,
                List.of(declinada, sinRespuesta))).doesNotThrowAnyException();
    }
}
