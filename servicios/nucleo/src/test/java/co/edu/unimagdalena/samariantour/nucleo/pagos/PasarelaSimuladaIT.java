package co.edu.unimagdalena.samariantour.nucleo.pagos;

import co.edu.unimagdalena.samariantour.nucleo.CatalogoSimulado;
import co.edu.unimagdalena.samariantour.nucleo.IntegracionPostgresBase;
import co.edu.unimagdalena.samariantour.nucleo.pagos.dominio.modelo.EstadoTransaccion;
import co.edu.unimagdalena.samariantour.nucleo.pagos.dominio.puerto.entrada.ProcesarEventoPagoUseCase;
import co.edu.unimagdalena.samariantour.nucleo.pagos.infraestructura.salida.pasarela.FormatoWompi;
import co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.modelo.SolicitudDisponibilidad;
import co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.puerto.salida.NotificacionRepositoryPort;
import co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.puerto.salida.SolicitudRepositoryPort;
import co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.puerto.salida.TransaccionPort;
import co.edu.unimagdalena.samariantour.seguridadjwt.TokensDePrueba;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static co.edu.unimagdalena.samariantour.nucleo.pagos.PagosDePrueba.PRECIO_CENTAVOS;
import static co.edu.unimagdalena.samariantour.nucleo.pagos.PagosDePrueba.SECRETO_SIMULADA;
import static co.edu.unimagdalena.samariantour.nucleo.solicitudes.SolicitudesDePrueba.AHORA;
import static co.edu.unimagdalena.samariantour.nucleo.solicitudes.SolicitudesDePrueba.MIERCOLES;
import static co.edu.unimagdalena.samariantour.nucleo.solicitudes.SolicitudesDePrueba.TURISTA;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * La página de la pasarela simulada (DN-18): al pulsar un botón, la pasarela envía unos milisegundos después un evento
 * con la forma del de Wompi, firmado con el secreto de demo. Aquí lo recibe un servidor de prueba en lugar del webhook,
 * para leerlo; después se entrega al webhook del núcleo y recorre el mismo camino que un pago real.
 */
@SpringBootTest(properties = {"nucleo.bandeja.publicacion.cron=-", "nucleo.solicitudes.vencimiento.cron=-",
        "nucleo.solicitudes.notificaciones.cron=-", "nucleo.solicitudes.idempotencia.cron=-",
        "nucleo.pagos.simulada.demora-evento=100ms"})
@AutoConfigureMockMvc
@ActiveProfiles("demo")
class PasarelaSimuladaIT extends IntegracionPostgresBase {

    private static final CatalogoSimulado PASARELA = new CatalogoSimulado();
    static final AtomicReference<Instant> AHORA_DEL_SERVICIO = new AtomicReference<>(AHORA);

    @TestConfiguration
    static class RelojDePrueba {
        @Bean
        @Primary
        Clock relojDePrueba() {
            return PagosDePrueba.reloj(AHORA_DEL_SERVICIO);
        }
    }

    @DynamicPropertySource
    static void webhookDePrueba(DynamicPropertyRegistry registro) {
        registro.add("nucleo.pagos.simulada.url-webhook", () -> PASARELA.url() + "/eventos");
    }

    @Autowired
    private MockMvc http;

    @Autowired
    private ObjectMapper json;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private SolicitudRepositoryPort solicitudes;

    @Autowired
    private NotificacionRepositoryPort notificaciones;

    @Autowired
    private TransaccionPort transaccion;

    @Autowired
    private ProcesarEventoPagoUseCase eventos;

    @BeforeEach
    void preparar() {
        for (String tabla : List.of("pagos_tardios", "dispersiones", "eventos_pasarela", "intentos_pago",
                "claves_idempotencia_pagos", "bandeja_salida", "notificaciones_solicitud", "transiciones_solicitud",
                "solicitudes")) {
            jdbc.update("DELETE FROM " + tabla);
        }
        PASARELA.reiniciar();
        PASARELA.responder(200);
        AHORA_DEL_SERVICIO.set(AHORA.plus(Duration.ofHours(2)));
    }

    @AfterAll
    static void apagar() {
        PASARELA.close();
    }

    @Test
    @DisplayName("la sesión abre la página simulada; Aprobar envía un evento firmado como el de Wompi y queda Pagada")
    void aprobarEnLaPagina() throws Exception {
        SolicitudDisponibilidad solicitud = guardar(PagosDePrueba.confirmada(MIERCOLES));
        JsonNode sesion = json.readTree(http.perform(post("/turista/solicitudes/{id}/pago", solicitud.id())
                        .header(HttpHeaders.AUTHORIZATION, TokensDePrueba.bearer(TURISTA, "TURISTA"))
                        .header("Idempotency-Key", UUID.randomUUID()))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString());
        String referencia = sesion.path("referencia").asText();
        String pagina = sesion.path("checkout").path("url").asText().replace("http://localhost:8080/api/nucleo", "");

        http.perform(get(pagina))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML))
                .andExpect(content().string(containsString("$300.000 COP")))
                .andExpect(content().string(containsString(referencia + "/aprobar")))
                .andExpect(content().string(containsString("Rechazar")));
        http.perform(post("/pasarela-simulada/{referencia}/aprobar", referencia)
                        .param("monto", Long.toString(PRECIO_CENTAVOS)).param("solicitud", solicitud.id().toString()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("aprobado")));

        String evento = esperarEvento();
        assertThat(FormatoWompi.verificar(evento, null, SECRETO_SIMULADA, json)).hasValueSatisfying(verificado -> {
            assertThat(verificado.referencia()).isEqualTo(referencia);
            assertThat(verificado.estado()).isEqualTo(EstadoTransaccion.APROBADA);
            assertThat(verificado.montoCentavos()).isEqualTo(PRECIO_CENTAVOS);
        });
        assertThat(jdbc.queryForObject("SELECT estado FROM solicitudes", String.class)).isEqualTo("CONFIRMADA");

        http.perform(post("/pagos/eventos").contentType(MediaType.APPLICATION_JSON).content(evento))
                .andExpect(status().isOk());
        eventos.procesarPendientes();

        assertThat(jdbc.queryForObject("SELECT estado FROM solicitudes", String.class)).isEqualTo("PAGADA");
    }

    @Test
    @DisplayName("una referencia que no es de un intento o un monto no positivo responden 400 sin enviar nada")
    void paginaInvalida() throws Exception {
        http.perform(get("/pasarela-simulada/otra").param("monto", "100").param("solicitud", UUID.randomUUID()
                .toString())).andExpect(status().isBadRequest());
        http.perform(post("/pasarela-simulada/{referencia}/rechazar", "ST-" + UUID.randomUUID())
                .param("monto", "0").param("solicitud", UUID.randomUUID().toString()))
                .andExpect(status().isBadRequest());

        Thread.sleep(300);
        assertThat(PASARELA.recibidas()).isEmpty();
    }

    private String esperarEvento() throws InterruptedException {
        for (int vuelta = 0; vuelta < 100 && PASARELA.recibidas().isEmpty(); vuelta++) {
            Thread.sleep(50);
        }
        assertThat(PASARELA.recibidas()).singleElement()
                .satisfies(peticion -> assertThat(peticion.ruta()).isEqualTo("/eventos"));
        return PASARELA.recibidas().getFirst().cuerpo();
    }

    private SolicitudDisponibilidad guardar(SolicitudDisponibilidad solicitud) {
        return transaccion.ejecutar(() -> {
            solicitudes.insertar(solicitud);
            notificaciones.registrar(solicitud.extraerNotificaciones());
            return solicitud;
        });
    }
}
