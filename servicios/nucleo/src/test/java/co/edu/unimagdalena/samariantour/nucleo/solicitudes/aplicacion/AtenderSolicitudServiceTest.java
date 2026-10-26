package co.edu.unimagdalena.samariantour.nucleo.solicitudes.aplicacion;

import co.edu.unimagdalena.samariantour.nucleo.compartido.concurrencia.ConflictoConcurrenciaException;
import co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.excepcion.ExcepcionDominio;
import co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.modelo.EstadoSolicitud;
import co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.modelo.NotificacionSolicitud;
import co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.modelo.Paginacion;
import co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.modelo.ResultadoAtencion;
import co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.modelo.SolicitudDisponibilidad;
import co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.modelo.TipoNotificacion;
import co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.modelo.Pagina;
import co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.puerto.entrada.comando.AtenderSolicitudCommand;
import org.assertj.core.api.ThrowableAssert;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static co.edu.unimagdalena.samariantour.nucleo.solicitudes.SolicitudesDePrueba.AHORA;
import static co.edu.unimagdalena.samariantour.nucleo.solicitudes.SolicitudesDePrueba.OPERADOR;
import static co.edu.unimagdalena.samariantour.nucleo.solicitudes.SolicitudesDePrueba.OTRO_OPERADOR;
import static co.edu.unimagdalena.samariantour.nucleo.solicitudes.SolicitudesDePrueba.pendiente;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AtenderSolicitudServiceTest {

    private static final Instant UNA_HORA_DESPUES = AHORA.plus(Duration.ofHours(1));

    private final DoblesSolicitudes.Repositorio repositorio = new DoblesSolicitudes.Repositorio();
    private final DoblesSolicitudes.Notificaciones notificaciones = new DoblesSolicitudes.Notificaciones();
    private final DoblesSolicitudes.TransaccionDirecta transaccion = new DoblesSolicitudes.TransaccionDirecta();
    private final SolicitudDisponibilidad solicitud = pendiente();

    @Test
    @DisplayName("atendida con precio, política y aparte: Atendida, guardada y notificada al turista (RF-45)")
    void atendida() {
        repositorio.insertar(solicitud);

        SolicitudDisponibilidad atendida = servicio(UNA_HORA_DESPUES).atender(atender(OPERADOR));

        assertThat(atendida.estado()).isEqualTo(EstadoSolicitud.ATENDIDA);
        assertThat(repositorio.buscar(solicitud.id()).orElseThrow().condiciones().precioBase()).isEqualTo(300_000);
        assertThat(notificaciones.registradas).extracting(NotificacionSolicitud::tipo)
                .containsExactly(TipoNotificacion.SOLICITUD_ATENDIDA);
    }

    @Test
    @DisplayName("no disponible, con su nota como motivo")
    void noDisponible() {
        repositorio.insertar(solicitud);

        servicio(UNA_HORA_DESPUES).atender(new AtenderSolicitudCommand(OPERADOR, solicitud.id(),
                ResultadoAtencion.NO_DISPONIBLE, null, null, null, "Sin cupo esa semana"));

        SolicitudDisponibilidad guardada = repositorio.buscar(solicitud.id()).orElseThrow();
        assertThat(guardada.estado()).isEqualTo(EstadoSolicitud.NO_DISPONIBLE);
        assertThat(guardada.historial().getLast().motivo()).isEqualTo("Sin cupo esa semana");
    }

    @Test
    @DisplayName("la de otro operador responde como si no existiera, y no la cambia")
    void otroOperador() {
        repositorio.insertar(solicitud);

        assertCodigo(() -> servicio(UNA_HORA_DESPUES).atender(atender(OTRO_OPERADOR)), "SOLICITUD_NO_ENCONTRADA");
        assertThat(repositorio.buscar(solicitud.id()).orElseThrow().estado()).isEqualTo(EstadoSolicitud.PENDIENTE);
    }

    @Test
    @DisplayName("atendida sin precio o sin el aparte acordado: DATO_INVALIDO (RF-37 a RF-39)")
    void faltanDatos() {
        assertCodigo(() -> new AtenderSolicitudCommand(OPERADOR, solicitud.id(), ResultadoAtencion.ATENDIDA, null,
                "Sin reembolso", true, null), "DATO_INVALIDO");
        assertCodigo(() -> new AtenderSolicitudCommand(OPERADOR, solicitud.id(), ResultadoAtencion.ATENDIDA, 300_000L,
                "Sin reembolso", false, null), "DATO_INVALIDO");
        assertCodigo(() -> new AtenderSolicitudCommand(OPERADOR, solicitud.id(), null, null, null, null, null),
                "DATO_INVALIDO");
    }

    @Test
    @DisplayName("si el vencimiento la cambió mientras tanto: reintenta, ve que ya está Sin respuesta y responde 409")
    void cruceConElVencimiento() {
        RepositorioConVencimiento conVencimiento = new RepositorioConVencimiento();
        conVencimiento.insertar(solicitud);
        AtenderSolicitudService servicio = new AtenderSolicitudService(conVencimiento, notificaciones, transaccion,
                DoblesSolicitudes.sinEspera(), DoblesSolicitudes.reloj(AHORA.plus(Duration.ofHours(23))));

        assertCodigo(() -> servicio.atender(atender(OPERADOR)), "TRANSICION_INVALIDA");

        assertThat(transaccion.abiertas).as("un intento por transacción").isEqualTo(2);
        assertThat(conVencimiento.buscar(solicitud.id()).orElseThrow().estado())
                .isEqualTo(EstadoSolicitud.SIN_RESPUESTA);
        assertThat(notificaciones.registradas).isEmpty();
    }

    @Test
    @DisplayName("la cola, el detalle con el WhatsApp del prestador y los avisos del operador")
    void consultar() {
        repositorio.insertar(solicitud);
        DoblesSolicitudes.Directorio directorio = new DoblesSolicitudes.Directorio();
        ConsultarColaOperadorService cola = new ConsultarColaOperadorService(repositorio, directorio,
                (operador, paginacion) -> new Pagina<>(List.of(), 0, paginacion.tamano(), 0));

        assertThat(cola.cola(OPERADOR, EstadoSolicitud.PENDIENTE, new Paginacion(0, 20)).contenido()).hasSize(1);
        assertThat(cola.cola(OTRO_OPERADOR, EstadoSolicitud.PENDIENTE, new Paginacion(0, 20)).contenido()).isEmpty();
        assertThat(cola.detalle(OPERADOR, solicitud.id()).whatsappPrestador()).isEqualTo("+573001110001");
        assertCodigo(() -> cola.detalle(OTRO_OPERADOR, solicitud.id()), "SOLICITUD_NO_ENCONTRADA");
        assertThat(cola.avisos(OPERADOR, new Paginacion(0, 5)).tamano()).isEqualTo(5);
    }

    private AtenderSolicitudService servicio(Instant ahora) {
        return new AtenderSolicitudService(repositorio, notificaciones, transaccion, DoblesSolicitudes.sinEspera(),
                DoblesSolicitudes.reloj(ahora));
    }

    private AtenderSolicitudCommand atender(UUID operador) {
        return new AtenderSolicitudCommand(operador, solicitud.id(), ResultadoAtencion.ATENDIDA, 300_000L,
                "Sin reembolso a menos de 24 h", true, "Incluye el ingreso al parque");
    }

    private static void assertCodigo(ThrowableAssert.ThrowingCallable accion, String codigo) {
        assertThatThrownBy(accion).isInstanceOf(ExcepcionDominio.class)
                .extracting(e -> ((ExcepcionDominio) e).codigo()).isEqualTo(codigo);
    }

    /** Un repositorio en el que, al primer guardado, el vencimiento ya había ganado: la deja Sin respuesta y choca. */
    private static final class RepositorioConVencimiento extends DoblesSolicitudes.Repositorio {

        private boolean primera = true;

        @Override
        public void actualizar(SolicitudDisponibilidad solicitud) {
            if (primera) {
                primera = false;
                SolicitudDisponibilidad delVencimiento = buscar(solicitud.id()).orElseThrow();
                delVencimiento.vencer(AHORA.plus(Duration.ofHours(24)));
                super.actualizar(delVencimiento);
                throw new ConflictoConcurrenciaException("el vencimiento la cambió primero");
            }
            super.actualizar(solicitud);
        }
    }
}
