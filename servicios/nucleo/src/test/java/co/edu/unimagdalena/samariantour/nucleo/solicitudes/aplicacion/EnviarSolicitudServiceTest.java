package co.edu.unimagdalena.samariantour.nucleo.solicitudes.aplicacion;

import co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.excepcion.CatalogoNoDisponibleException;
import co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.excepcion.ExcepcionDominio;
import co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.modelo.EstadoSolicitud;
import co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.modelo.NotificacionSolicitud;
import co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.modelo.PeticionIdempotente;
import co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.modelo.RespuestaGuardada;
import co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.modelo.ResultadoIdempotente;
import co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.modelo.SolicitudDisponibilidad;
import co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.modelo.TipoNotificacion;
import co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.puerto.entrada.comando.EnviarSolicitudCommand;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static co.edu.unimagdalena.samariantour.nucleo.solicitudes.SolicitudesDePrueba.AGENCIA;
import static co.edu.unimagdalena.samariantour.nucleo.solicitudes.SolicitudesDePrueba.AHORA;
import static co.edu.unimagdalena.samariantour.nucleo.solicitudes.SolicitudesDePrueba.CABANA;
import static co.edu.unimagdalena.samariantour.nucleo.solicitudes.SolicitudesDePrueba.CABO_SAN_JUAN;
import static co.edu.unimagdalena.samariantour.nucleo.solicitudes.SolicitudesDePrueba.HOTEL;
import static co.edu.unimagdalena.samariantour.nucleo.solicitudes.SolicitudesDePrueba.MIERCOLES;
import static co.edu.unimagdalena.samariantour.nucleo.solicitudes.SolicitudesDePrueba.OPERADOR;
import static co.edu.unimagdalena.samariantour.nucleo.solicitudes.SolicitudesDePrueba.OTRO_TURISTA;
import static co.edu.unimagdalena.samariantour.nucleo.solicitudes.SolicitudesDePrueba.SIETE;
import static co.edu.unimagdalena.samariantour.nucleo.solicitudes.SolicitudesDePrueba.TURISTA;
import static co.edu.unimagdalena.samariantour.nucleo.solicitudes.SolicitudesDePrueba.cabana;
import static co.edu.unimagdalena.samariantour.nucleo.solicitudes.SolicitudesDePrueba.caminata;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EnviarSolicitudServiceTest {

    private final DoblesSolicitudes.Repositorio repositorio = new DoblesSolicitudes.Repositorio();
    private final DoblesSolicitudes.Notificaciones notificaciones = new DoblesSolicitudes.Notificaciones();
    private final DoblesSolicitudes.Catalogo catalogo = new DoblesSolicitudes.Catalogo().con(caminata())
            .con(cabana());
    private final DoblesSolicitudes.Directorio directorio = new DoblesSolicitudes.Directorio();
    private final EnviarSolicitudService servicio = new EnviarSolicitudService(repositorio, catalogo, directorio,
            directorio, notificaciones, new EjecucionIdempotente(new DoblesSolicitudes.Claves(),
            new DoblesSolicitudes.TransaccionDirecta(), DoblesSolicitudes.sinEspera(), DoblesSolicitudes.reloj(AHORA)),
            DoblesSolicitudes.reloj(AHORA));

    private final AtomicReference<SolicitudDisponibilidad> creada = new AtomicReference<>();

    @Test
    @DisplayName("queda Pendiente con el operador del prestador, con sus notificaciones al turista y al operador")
    void enviar() {
        ResultadoIdempotente resultado = enviar(UUID.randomUUID(), comando(TURISTA, 2));

        assertThat(resultado.repetida()).isFalse();
        assertThat(resultado.respuesta().codigo()).isEqualTo(201);
        SolicitudDisponibilidad guardada = repositorio.buscar(creada.get().id()).orElseThrow();
        assertThat(guardada.estado()).isEqualTo(EstadoSolicitud.PENDIENTE);
        assertThat(guardada.operadorId()).isEqualTo(OPERADOR);
        assertThat(guardada.oferta().prestadorId()).isEqualTo(AGENCIA);
        assertThat(notificaciones.registradas).extracting(NotificacionSolicitud::tipo)
                .containsExactly(TipoNotificacion.SOLICITUD_RECIBIDA, TipoNotificacion.NUEVA_SOLICITUD);
    }

    @Test
    @DisplayName("con la misma clave, la segunda vez repite la respuesta sin consultar al catálogo ni crear otra")
    void mismaClave() {
        UUID clave = UUID.randomUUID();
        ResultadoIdempotente primera = enviar(clave, comando(TURISTA, 2));

        ResultadoIdempotente segunda = enviar(clave, comando(TURISTA, 2));

        assertThat(segunda.repetida()).isTrue();
        assertThat(segunda.respuesta()).isEqualTo(primera.respuesta());
        assertThat(repositorio.cantidad()).isEqualTo(1);
        assertThat(catalogo.consultas).isEqualTo(1);
    }

    @Test
    @DisplayName("otra clave con lo mismo: SOLICITUD_DUPLICADA (RF-31)")
    void duplicada() {
        enviar(UUID.randomUUID(), comando(TURISTA, 2));

        assertCodigo(() -> enviar(UUID.randomUUID(), comando(TURISTA, 3)), "SOLICITUD_DUPLICADA");
        assertThat(repositorio.cantidad()).isEqualTo(1);
    }

    @Test
    @DisplayName("oferta no visible: OFERTA_NO_ENCONTRADA; catálogo caído: CATALOGO_NO_DISPONIBLE; sin crear nada")
    void catalogo() {
        assertCodigo(() -> enviar(UUID.randomUUID(), new EnviarSolicitudCommand(TURISTA, "b000000000000000000000ff",
                MIERCOLES, SIETE, null, null, null, 2, null)), "OFERTA_NO_ENCONTRADA");
        catalogo.falla = new CatalogoNoDisponibleException("no respondió a tiempo");
        assertCodigo(() -> enviar(UUID.randomUUID(), comando(TURISTA, 2)), "CATALOGO_NO_DISPONIBLE");

        assertThat(repositorio.cantidad()).isZero();
        assertThat(notificaciones.registradas).isEmpty();
    }

    @Test
    @DisplayName("sin WhatsApp verificado: WHATSAPP_NO_VERIFICADO, sin consultar al catálogo")
    void sinWhatsapp() {
        assertCodigo(() -> enviar(UUID.randomUUID(), comando(OTRO_TURISTA, 2)), "WHATSAPP_NO_VERIFICADO");
        assertThat(catalogo.consultas).isZero();
    }

    @Test
    @DisplayName("prestador sin operador asignado: PRESTADOR_SIN_OPERADOR (RF-32)")
    void sinOperador() {
        directorio.operadores.clear();

        assertCodigo(() -> enviar(UUID.randomUUID(), comando(TURISTA, 2)), "PRESTADOR_SIN_OPERADOR");
    }

    @Test
    @DisplayName("un dato inválido de la solicitud (más personas que la capacidad): DATO_INVALIDO")
    void datoInvalido() {
        assertCodigo(() -> enviar(UUID.randomUUID(), comando(TURISTA, 20)), "DATO_INVALIDO");
        assertCodigo(() -> new EnviarSolicitudCommand(TURISTA, " ", MIERCOLES, SIETE, null, null, null, 2, null),
                "DATO_INVALIDO");
    }

    @Test
    @DisplayName("los campos de cada tipo llegan a la solicitud: alojamiento con su salida y sus unidades (RF-30)")
    void alojamiento() {
        directorio.operadores.put(HOTEL, OPERADOR);
        EnviarSolicitudCommand cabana = new EnviarSolicitudCommand(TURISTA, CABANA, MIERCOLES, null, null,
                MIERCOLES.plusDays(2), 1, 3, null);

        enviar(UUID.randomUUID(), cabana);

        SolicitudDisponibilidad guardada = repositorio.buscar(creada.get().id()).orElseThrow();
        assertThat(guardada.fechaSalida()).isEqualTo(MIERCOLES.plusDays(2));
        assertThat(guardada.numeroUnidades()).isEqualTo(1);
        assertCodigo(() -> enviar(UUID.randomUUID(), new EnviarSolicitudCommand(TURISTA, CABANA,
                MIERCOLES.plusDays(1), SIETE, null, MIERCOLES.plusDays(2), 1, 3, null)), "DATO_INVALIDO");
        assertCodigo(() -> enviar(UUID.randomUUID(), new EnviarSolicitudCommand(TURISTA, CABANA, MIERCOLES, null,
                null, MIERCOLES.plusDays(4), 2, 2, null)), "SOLICITUD_DUPLICADA");
    }

    private ResultadoIdempotente enviar(UUID clave, EnviarSolicitudCommand comando) {
        return servicio.enviar(comando, PeticionIdempotente.de(clave, comando.turistaId(), "ENVIAR_SOLICITUD",
                comando.toString()), solicitud -> {
            creada.set(solicitud);
            return new RespuestaGuardada(201, "{\"id\":\"" + solicitud.id() + "\"}");
        });
    }

    private static EnviarSolicitudCommand comando(UUID turista, int personas) {
        return new EnviarSolicitudCommand(turista, CABO_SAN_JUAN, MIERCOLES, SIETE, null, null, null, personas,
                "Con una niña");
    }

    private static void assertCodigo(org.assertj.core.api.ThrowableAssert.ThrowingCallable accion, String codigo) {
        assertThatThrownBy(accion).isInstanceOf(ExcepcionDominio.class)
                .extracting(e -> ((ExcepcionDominio) e).codigo()).isEqualTo(codigo);
    }
}
