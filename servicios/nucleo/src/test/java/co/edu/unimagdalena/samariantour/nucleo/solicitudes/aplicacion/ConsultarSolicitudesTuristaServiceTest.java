package co.edu.unimagdalena.samariantour.nucleo.solicitudes.aplicacion;

import co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.excepcion.SolicitudNoEncontradaException;
import co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.modelo.CamposDelTipo;
import co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.modelo.Paginacion;
import co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.modelo.SolicitudDisponibilidad;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static co.edu.unimagdalena.samariantour.nucleo.solicitudes.SolicitudesDePrueba.AHORA;
import static co.edu.unimagdalena.samariantour.nucleo.solicitudes.SolicitudesDePrueba.MIERCOLES;
import static co.edu.unimagdalena.samariantour.nucleo.solicitudes.SolicitudesDePrueba.OPERADOR;
import static co.edu.unimagdalena.samariantour.nucleo.solicitudes.SolicitudesDePrueba.OTRO_TURISTA;
import static co.edu.unimagdalena.samariantour.nucleo.solicitudes.SolicitudesDePrueba.SIETE;
import static co.edu.unimagdalena.samariantour.nucleo.solicitudes.SolicitudesDePrueba.TURISTA;
import static co.edu.unimagdalena.samariantour.nucleo.solicitudes.SolicitudesDePrueba.caminata;
import static co.edu.unimagdalena.samariantour.nucleo.solicitudes.SolicitudesDePrueba.pendiente;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ConsultarSolicitudesTuristaServiceTest {

    private final DoblesSolicitudes.Repositorio repositorio = new DoblesSolicitudes.Repositorio();
    private final ConsultarSolicitudesTuristaService servicio = new ConsultarSolicitudesTuristaService(repositorio);

    @Test
    @DisplayName("consulta la suya con el historial; la de otro turista responde como si no existiera")
    void consultar() {
        SolicitudDisponibilidad suya = pendiente();
        repositorio.insertar(suya);

        assertThat(servicio.consultar(TURISTA, suya.id()).historial()).hasSize(1);
        assertThatThrownBy(() -> servicio.consultar(OTRO_TURISTA, suya.id()))
                .isInstanceOf(SolicitudNoEncontradaException.class)
                .extracting(e -> ((SolicitudNoEncontradaException) e).codigo()).isEqualTo("SOLICITUD_NO_ENCONTRADA");
        assertThatThrownBy(() -> servicio.consultar(TURISTA, UUID.randomUUID()))
                .isInstanceOf(SolicitudNoEncontradaException.class);
    }

    @Test
    @DisplayName("lista solo las suyas, de la más reciente a la más antigua")
    void listar() {
        SolicitudDisponibilidad vieja = pendiente();
        SolicitudDisponibilidad nueva = SolicitudDisponibilidad.crear(UUID.randomUUID(), TURISTA, caminata(),
                MIERCOLES.plusDays(1), CamposDelTipo.actividad(SIETE), 2, null, OPERADOR, AHORA.plusSeconds(60));
        SolicitudDisponibilidad ajena = SolicitudDisponibilidad.crear(UUID.randomUUID(), OTRO_TURISTA, caminata(),
                MIERCOLES, CamposDelTipo.actividad(SIETE), 2, null, OPERADOR, AHORA);
        repositorio.insertar(vieja);
        repositorio.insertar(nueva);
        repositorio.insertar(ajena);

        assertThat(servicio.listar(TURISTA, new Paginacion(0, 20)).contenido()).extracting(SolicitudDisponibilidad::id)
                .containsExactly(nueva.id(), vieja.id());
    }
}
