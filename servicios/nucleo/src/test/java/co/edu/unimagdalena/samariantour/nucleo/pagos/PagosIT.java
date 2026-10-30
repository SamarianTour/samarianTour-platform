package co.edu.unimagdalena.samariantour.nucleo.pagos;

import co.edu.unimagdalena.samariantour.nucleo.IntegracionPostgresBase;
import co.edu.unimagdalena.samariantour.nucleo.pagos.dominio.puerto.entrada.EjecutarDispersionesUseCase;
import co.edu.unimagdalena.samariantour.nucleo.pagos.dominio.puerto.entrada.ProcesarEventoPagoUseCase;
import co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.modelo.SolicitudDisponibilidad;
import co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.puerto.entrada.VencerSolicitudesUseCase;
import co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.puerto.salida.NotificacionRepositoryPort;
import co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.puerto.salida.SolicitudRepositoryPort;
import co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.puerto.salida.TransaccionPort;
import co.edu.unimagdalena.samariantour.seguridadjwt.TokensDePrueba;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicReference;

import static co.edu.unimagdalena.samariantour.nucleo.pagos.PagosDePrueba.ADMINISTRADOR;
import static co.edu.unimagdalena.samariantour.nucleo.pagos.PagosDePrueba.FIN_VENTANA;
import static co.edu.unimagdalena.samariantour.nucleo.pagos.PagosDePrueba.PRECIO_CENTAVOS;
import static co.edu.unimagdalena.samariantour.nucleo.pagos.PagosDePrueba.RESTAURANTE;
import static co.edu.unimagdalena.samariantour.nucleo.pagos.PagosDePrueba.confirmada;
import static co.edu.unimagdalena.samariantour.nucleo.pagos.PagosDePrueba.confirmadaRestaurante;
import static co.edu.unimagdalena.samariantour.nucleo.solicitudes.SolicitudesDePrueba.AHORA;
import static co.edu.unimagdalena.samariantour.nucleo.solicitudes.SolicitudesDePrueba.MIERCOLES;
import static co.edu.unimagdalena.samariantour.nucleo.solicitudes.SolicitudesDePrueba.OTRO_TURISTA;
import static co.edu.unimagdalena.samariantour.nucleo.solicitudes.SolicitudesDePrueba.TURISTA;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Pagos de punta a punta con PostgreSQL y la pasarela simulada (RF-46 a RF-58, DN-18 a DN-27): la sesión de cobro con
 * su clave, el webhook, el procesamiento con las solicitudes en la misma transacción, la dispersión y la comisión. Las
 * tareas programadas no corren: la prueba llama a cada caso de uso, con un reloj que mueve a mano.
 */
@SpringBootTest(properties = {"nucleo.bandeja.publicacion.cron=-", "nucleo.solicitudes.vencimiento.cron=-",
        "nucleo.solicitudes.notificaciones.cron=-", "nucleo.solicitudes.idempotencia.cron=-"})
@AutoConfigureMockMvc
@ActiveProfiles("demo")
class PagosIT extends IntegracionPostgresBase {

    static final AtomicReference<Instant> AHORA_DEL_SERVICIO = new AtomicReference<>(AHORA);

    @TestConfiguration
    static class RelojDePrueba {
        @Bean
        @Primary
        Clock relojDePrueba() {
            return PagosDePrueba.reloj(AHORA_DEL_SERVICIO);
        }
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

    @Autowired
    private EjecutarDispersionesUseCase dispersiones;

    @Autowired
    private VencerSolicitudesUseCase vencimientos;

    @BeforeEach
    void vaciar() {
        for (String tabla : List.of("pagos_tardios", "dispersiones", "eventos_pasarela", "intentos_pago",
                "claves_idempotencia_pagos", "bandeja_salida", "notificaciones_solicitud", "transiciones_solicitud",
                "solicitudes")) {
            jdbc.update("DELETE FROM " + tabla);
        }
        jdbc.update("DELETE FROM configuracion_comision WHERE administrador_id IS NOT NULL");
        AHORA_DEL_SERVICIO.set(AHORA.plus(Duration.ofHours(2)));
    }

    @Test
    @DisplayName("sesión de cobro: misma clave, misma respuesta; otra clave devuelve el intento en curso")
    void sesionIdempotente() throws Exception {
        UUID id = guardar(confirmada(MIERCOLES)).id();
        String clave = UUID.randomUUID().toString();

        String primera = pagar(id, clave).andExpect(status().isCreated())
                .andExpect(header().doesNotExist("Idempotency-Replayed"))
                .andExpect(jsonPath("$.numero").value(1))
                .andExpect(jsonPath("$.estado").value("EN_CURSO"))
                .andExpect(jsonPath("$.montoCentavos").value(PRECIO_CENTAVOS))
                .andExpect(jsonPath("$.moneda").value("COP"))
                .andExpect(jsonPath("$.checkout.pasarela").value("SIMULADA"))
                .andReturn().getResponse().getContentAsString();
        String repetida = pagar(id, clave).andExpect(status().isCreated())
                .andExpect(header().string("Idempotency-Replayed", "true"))
                .andReturn().getResponse().getContentAsString();
        pagar(id, UUID.randomUUID().toString()).andExpect(status().isCreated())
                .andExpect(jsonPath("$.intentoId").value(json.readTree(primera).path("intentoId").asText()));

        assertThat(repetida).isEqualTo(primera);
        assertThat(contar("intentos_pago")).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT intento_pago_en_curso FROM solicitudes", Boolean.class)).isTrue();
        assertThat(json.readTree(primera).path("checkout").path("url").asText())
                .contains("/pasarela-simulada/ST-", "monto=" + PRECIO_CENTAVOS);
    }

    @Test
    @DisplayName("sin clave 400; de otro turista 404; atendida sin confirmar 409 SOLICITUD_NO_COBRABLE")
    void rechazosDeLaSesion() throws Exception {
        UUID id = guardar(confirmada(MIERCOLES)).id();

        http.perform(comoTurista(post("/turista/solicitudes/{id}/pago", id), TURISTA))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.codigo").value("DATO_INVALIDO"));
        http.perform(comoTurista(post("/turista/solicitudes/{id}/pago", id), OTRO_TURISTA)
                        .header("Idempotency-Key", UUID.randomUUID()))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.codigo").value("SOLICITUD_NO_ENCONTRADA"));
        http.perform(comoTurista(get("/turista/solicitudes/{id}/pagos", id), OTRO_TURISTA))
                .andExpect(status().isNotFound());
        UUID sinConfirmar = guardar(PagosDePrueba.atendida(MIERCOLES.plusDays(1))).id();
        pagar(sinConfirmar, UUID.randomUUID().toString()).andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("SOLICITUD_NO_COBRABLE"));
        assertThat(contar("intentos_pago")).isZero();
    }

    @Test
    @DisplayName("aprobado: el webhook lo guarda una vez y al procesarlo queda Pagada, con dispersión, aviso y opinión")
    void pagoAprobado() throws Exception {
        UUID id = guardar(confirmada(MIERCOLES)).id();
        String referencia = referencia(pagar(id, UUID.randomUUID().toString()));
        String evento = PagosDePrueba.evento("T-1", referencia, PRECIO_CENTAVOS, "APPROVED", json);

        webhook(evento).andExpect(status().isOk()).andExpect(jsonPath("$.duplicado").value(false));
        webhook(evento).andExpect(status().isOk()).andExpect(jsonPath("$.duplicado").value(true));
        assertThat(contar("eventos_pasarela")).isEqualTo(1);

        assertThat(eventos.procesarPendientes()).isEqualTo(1);
        assertThat(eventos.procesarPendientes()).isZero();

        assertThat(jdbc.queryForObject("SELECT estado FROM solicitudes", String.class)).isEqualTo("PAGADA");
        assertThat(jdbc.queryForObject("SELECT estado || ' ' || transaccion_id FROM intentos_pago", String.class))
                .isEqualTo("APROBADO T-1");
        assertThat(jdbc.queryForObject("SELECT resultado FROM eventos_pasarela", String.class)).isEqualTo("APLICADO");
        assertThat(jdbc.queryForObject("SELECT porcentaje_comision || ' ' || comision || ' ' || costo_transaccional "
                + "|| ' ' || monto_neto || ' ' || estado FROM dispersiones", String.class))
                .isEqualTo("4.00 12000 10294 277706 PENDIENTE");
        assertThat(jdbc.queryForObject("SELECT cuerpo ->> 'tipo' FROM bandeja_salida", String.class))
                .isEqualTo("HABILITACION_OPINION");
        assertThat(jdbc.queryForList("SELECT destinatario_tipo FROM notificaciones_solicitud "
                + "WHERE tipo = 'SOLICITUD_PAGADA'", String.class)).containsExactlyInAnyOrder("TURISTA", "PRESTADOR");
        http.perform(comoTurista(get("/turista/solicitudes/{id}/pagos", id), TURISTA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].estado").value("APROBADO"))
                .andExpect(jsonPath("$[0].transaccionId").value("T-1"));
    }

    @Test
    @DisplayName("firma inválida: 401 y no se guarda nada (RF-50)")
    void firmaInvalida() throws Exception {
        UUID id = guardar(confirmada(MIERCOLES)).id();
        String referencia = referencia(pagar(id, UUID.randomUUID().toString()));
        String evento = PagosDePrueba.evento("T-1", referencia, PRECIO_CENTAVOS, "APPROVED", json)
                .replace("\"APPROVED\"", "\"DECLINED\"");

        webhook(evento).andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.codigo").value("EVENTO_NO_AUTENTICO"));
        webhook("{}").andExpect(status().isUnauthorized());

        assertThat(contar("eventos_pasarela")).isZero();
    }

    @Test
    @DisplayName("el pago y la solicitud se confirman juntos: si falla una parte, no queda ninguna")
    void todoONada() throws Exception {
        UUID id = guardar(confirmada(MIERCOLES)).id();
        String referencia = referencia(pagar(id, UUID.randomUUID().toString()));
        webhook(PagosDePrueba.evento("T-1", referencia, PRECIO_CENTAVOS, "APPROVED", json));
        // Una dispersión que ya ocupa la solicitud hace fallar la última escritura del procesamiento.
        jdbc.update("""
                INSERT INTO dispersiones (id, solicitud_id, prestador_id, monto_bruto, porcentaje_comision, comision,
                                          costo_transaccional, monto_neto, estado, intentos, creada_en)
                VALUES (?, ?, ?, 1, 0, 0, 0, 1, 'EXITOSA', 1, now())""", UUID.randomUUID(), id, UUID.randomUUID());

        assertThatThrownBy(() -> eventos.procesarPendientes()).isInstanceOf(RuntimeException.class);

        assertThat(jdbc.queryForObject("SELECT estado FROM solicitudes", String.class)).isEqualTo("CONFIRMADA");
        assertThat(jdbc.queryForObject("SELECT estado FROM intentos_pago", String.class)).isEqualTo("EN_CURSO");
        assertThat(jdbc.queryForObject("SELECT count(*) FROM eventos_pasarela WHERE procesado_en IS NULL",
                Integer.class)).isEqualTo(1);
        assertThat(contar("bandeja_salida")).isZero();
    }

    @Test
    @DisplayName("aprobado sobre una vencida: pago tardío para el administrador, que lo ve y recibe el aviso (RF-50)")
    void pagoTardio() throws Exception {
        UUID id = guardar(confirmada(MIERCOLES)).id();
        String referencia = referencia(pagar(id, UUID.randomUUID().toString()));
        webhook(PagosDePrueba.evento("T-1", referencia, PRECIO_CENTAVOS, "APPROVED", json));
        AHORA_DEL_SERVICIO.set(FIN_VENTANA.plus(Duration.ofMinutes(31)));

        eventos.procesarPendientes();

        assertThat(jdbc.queryForObject("SELECT estado FROM solicitudes", String.class)).isEqualTo("VENCIDA");
        assertThat(jdbc.queryForObject("SELECT resultado FROM eventos_pasarela", String.class))
                .isEqualTo("PAGO_TARDIO");
        assertThat(contar("dispersiones")).isZero();
        assertThat(contar("bandeja_salida")).isZero();
        http.perform(comoAdministrador(get("/admin/pagos-tardios")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].solicitudId").value(id.toString()))
                .andExpect(jsonPath("$[0].monto").value(300_000))
                .andExpect(jsonPath("$[0].transaccionId").value("T-1"))
                .andExpect(jsonPath("$[0].avisado").value(false));

        assertThat(dispersiones.enviarAvisos()).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT aviso_enviado_en IS NOT NULL FROM pagos_tardios", Boolean.class))
                .isTrue();
    }

    @Test
    @DisplayName("rechazado y luego aprobado con otra transacción: la solicitud no cambia y queda el pago tardío")
    void aprobadoDespuesDelRechazo() throws Exception {
        UUID id = guardar(confirmada(MIERCOLES)).id();
        String referencia = referencia(pagar(id, UUID.randomUUID().toString()));
        webhook(PagosDePrueba.evento("T-1", referencia, PRECIO_CENTAVOS, "DECLINED", json));
        assertThat(eventos.procesarPendientes()).isEqualTo(1);
        webhook(PagosDePrueba.evento("T-2", referencia, PRECIO_CENTAVOS, "APPROVED", json));

        assertThat(eventos.procesarPendientes()).isEqualTo(1);

        assertThat(jdbc.queryForObject("SELECT estado FROM solicitudes", String.class)).isEqualTo("PAGO_FALLIDO");
        assertThat(jdbc.queryForList("SELECT transaccion_id || ' ' || resultado FROM eventos_pasarela", String.class))
                .containsExactlyInAnyOrder("T-1 APLICADO", "T-2 PAGO_TARDIO");
        assertThat(jdbc.queryForObject("SELECT estado || ' ' || transaccion_id FROM intentos_pago", String.class))
                .isEqualTo("RECHAZADO T-1");
        assertThat(jdbc.queryForObject("SELECT transaccion_id FROM pagos_tardios", String.class)).isEqualTo("T-2");
        assertThat(contar("dispersiones")).isZero();
    }

    @Test
    @DisplayName("tres rechazos: Pago fallido, reintento con el número siguiente y Vencida al tercero (RF-58)")
    void tresRechazos() throws Exception {
        UUID id = guardar(confirmada(MIERCOLES)).id();

        for (int intento = 1; intento <= 3; intento++) {
            ResultActions sesion = pagar(id, UUID.randomUUID().toString()).andExpect(status().isCreated())
                    .andExpect(jsonPath("$.numero").value(intento));
            webhook(PagosDePrueba.evento("T-" + intento, referencia(sesion), PRECIO_CENTAVOS, "DECLINED", json));
            eventos.procesarPendientes();
            assertThat(jdbc.queryForObject("SELECT estado FROM solicitudes", String.class))
                    .isEqualTo(intento < 3 ? "PAGO_FALLIDO" : "VENCIDA");
        }

        pagar(id, UUID.randomUUID().toString()).andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("SOLICITUD_NO_COBRABLE"));
        assertThat(jdbc.queryForList("SELECT estado FROM intentos_pago ORDER BY numero", String.class))
                .containsExactly("RECHAZADO", "RECHAZADO", "RECHAZADO");
    }

    @Test
    @DisplayName("un intento en curso aplaza el vencimiento hasta 30 min; abandonado, la solicitud vence (RF-55)")
    void abandonado() throws Exception {
        UUID id = guardar(confirmada(MIERCOLES)).id();
        AHORA_DEL_SERVICIO.set(FIN_VENTANA.minus(Duration.ofMinutes(5)));
        pagar(id, UUID.randomUUID().toString()).andExpect(status().isCreated());

        AHORA_DEL_SERVICIO.set(FIN_VENTANA.plus(Duration.ofMinutes(10)));
        vencimientos.vencerLote();
        assertThat(jdbc.queryForObject("SELECT estado FROM solicitudes", String.class)).isEqualTo("CONFIRMADA");

        AHORA_DEL_SERVICIO.set(FIN_VENTANA.plus(Duration.ofMinutes(25)));
        assertThat(eventos.abandonarIntentos()).isEqualTo(1);
        vencimientos.vencerLote();

        assertThat(jdbc.queryForObject("SELECT estado FROM intentos_pago", String.class)).isEqualTo("ABANDONADO");
        assertThat(jdbc.queryForObject("SELECT estado FROM solicitudes", String.class)).isEqualTo("VENCIDA");
    }

    @Test
    @DisplayName("dos réplicas procesan eventos y dispersiones a la vez sin repetir ninguno")
    void dosReplicas() throws Exception {
        List<String> referencias = new ArrayList<>();
        for (int dia = 0; dia < 6; dia++) {
            UUID id = guardar(confirmada(MIERCOLES.plusDays(dia))).id();
            referencias.add(referencia(pagar(id, UUID.randomUUID().toString())));
        }
        for (int i = 0; i < referencias.size(); i++) {
            webhook(PagosDePrueba.evento("T-" + i, referencias.get(i), PRECIO_CENTAVOS, "APPROVED", json));
        }

        aLaVez(() -> eventos.procesarPendientes());
        aLaVez(() -> dispersiones.ejecutarPendientes());

        assertThat(jdbc.queryForObject("SELECT count(*) FROM eventos_pasarela WHERE resultado = 'APLICADO'",
                Integer.class)).isEqualTo(6);
        assertThat(jdbc.queryForList("SELECT DISTINCT estado FROM solicitudes", String.class))
                .containsExactly("PAGADA");
        assertThat(contar("bandeja_salida")).isEqualTo(6);
        assertThat(jdbc.queryForList("SELECT estado || ' ' || intentos FROM dispersiones", String.class))
                .hasSize(6).containsOnly("EXITOSA 1");
        assertThat(jdbc.queryForList("SELECT cuenta_destino FROM dispersiones", String.class))
                .containsOnly("******0101");
    }

    @Test
    @DisplayName("dispersión fallida: tres reintentos con espera creciente, fallida y aviso al prestador (RF-54)")
    void dispersionFallida() throws Exception {
        UUID id = guardar(confirmadaRestaurante(MIERCOLES)).id();
        String referencia = referencia(pagar(id, UUID.randomUUID().toString()));
        webhook(PagosDePrueba.evento("T-1", referencia, PRECIO_CENTAVOS, "APPROVED", json));
        eventos.procesarPendientes();

        for (int intento = 1; intento <= 4; intento++) {
            AHORA_DEL_SERVICIO.set(AHORA.plus(Duration.ofHours(2 + intento)));
            assertThat(dispersiones.ejecutarPendientes()).isEqualTo(1);
        }
        assertThat(dispersiones.ejecutarPendientes()).isZero();

        assertThat(jdbc.queryForObject("SELECT estado || ' ' || intentos || ' ' || cuenta_destino FROM dispersiones",
                String.class)).isEqualTo("FALLIDA 4 ******0404");
        assertThat(jdbc.queryForObject("SELECT prestador_id FROM dispersiones", UUID.class)).isEqualTo(RESTAURANTE);
        assertThat(dispersiones.enviarAvisos()).isEqualTo(1);
        assertThat(dispersiones.enviarAvisos()).isZero();
    }

    @Test
    @DisplayName("comisión: el administrador la cambia, rige para los pagos siguientes y no toca los anteriores")
    void comision() throws Exception {
        UUID antes = guardar(confirmada(MIERCOLES)).id();
        UUID despues = guardar(confirmada(MIERCOLES.plusDays(1))).id();
        String referenciaAntes = referencia(pagar(antes, UUID.randomUUID().toString()));
        String referenciaDespues = referencia(pagar(despues, UUID.randomUUID().toString()));
        webhook(PagosDePrueba.evento("T-1", referenciaAntes, PRECIO_CENTAVOS, "APPROVED", json));
        eventos.procesarPendientes();

        http.perform(comoAdministrador(get("/admin/comision")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.porcentaje").value(4.0));
        http.perform(comoAdministrador(put("/admin/comision")).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"porcentaje\": 5.5}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.porcentaje").value(5.5))
                .andExpect(jsonPath("$.historial.length()").value(2))
                .andExpect(jsonPath("$.historial[0].administradorId").value(ADMINISTRADOR.toString()));
        http.perform(comoAdministrador(put("/admin/comision")).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"porcentaje\": 150}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.codigo").value("DATO_INVALIDO"));
        http.perform(get("/admin/comision")
                        .header(HttpHeaders.AUTHORIZATION,
                                TokensDePrueba.bearer(UUID.randomUUID(), "OPERADOR_DISPONIBILIDAD")))
                .andExpect(status().isForbidden());
        webhook(PagosDePrueba.evento("T-2", referenciaDespues, PRECIO_CENTAVOS, "APPROVED", json));
        eventos.procesarPendientes();

        assertThat(jdbc.queryForObject("SELECT porcentaje_comision || ' ' || comision FROM dispersiones "
                + "WHERE solicitud_id = ?", String.class, antes)).isEqualTo("4.00 12000");
        assertThat(jdbc.queryForObject("SELECT porcentaje_comision || ' ' || comision FROM dispersiones "
                + "WHERE solicitud_id = ?", String.class, despues)).isEqualTo("5.50 16500");
        assertThat(jdbc.queryForObject("SELECT porcentaje FROM configuracion_comision ORDER BY vigente_desde DESC "
                + "LIMIT 1", BigDecimal.class)).isEqualByComparingTo("5.5");
    }

    private SolicitudDisponibilidad guardar(SolicitudDisponibilidad solicitud) {
        return transaccion.ejecutar(() -> {
            solicitudes.insertar(solicitud);
            notificaciones.registrar(solicitud.extraerNotificaciones());
            return solicitud;
        });
    }

    private ResultActions pagar(UUID solicitudId, String clave) throws Exception {
        return http.perform(comoTurista(post("/turista/solicitudes/{id}/pago", solicitudId), TURISTA)
                .header("Idempotency-Key", clave));
    }

    private String referencia(ResultActions sesion) throws Exception {
        MvcResult resultado = sesion.andExpect(status().isCreated()).andReturn();
        JsonNode cuerpo = json.readTree(resultado.getResponse().getContentAsString());
        return cuerpo.path("referencia").asText();
    }

    private ResultActions webhook(String evento) throws Exception {
        return http.perform(post("/pagos/eventos").contentType(MediaType.APPLICATION_JSON).content(evento));
    }

    private int contar(String tabla) {
        return jdbc.queryForObject("SELECT count(*) FROM " + tabla, Integer.class);
    }

    /** Lo mismo en dos hilos que arrancan juntos, como dos réplicas. */
    private static void aLaVez(Callable<Integer> tarea) throws Exception {
        ExecutorService hilos = Executors.newFixedThreadPool(2);
        CountDownLatch salida = new CountDownLatch(1);
        try {
            List<Future<Integer>> resultados = new ArrayList<>();
            for (int i = 0; i < 2; i++) {
                resultados.add(hilos.submit(() -> {
                    salida.await();
                    return tarea.call();
                }));
            }
            salida.countDown();
            for (Future<Integer> resultado : resultados) {
                resultado.get();
            }
        } finally {
            hilos.shutdown();
        }
    }

    private static MockHttpServletRequestBuilder comoTurista(MockHttpServletRequestBuilder peticion, UUID turista) {
        return peticion.header(HttpHeaders.AUTHORIZATION, TokensDePrueba.bearer(turista, "TURISTA"));
    }

    private static MockHttpServletRequestBuilder comoAdministrador(MockHttpServletRequestBuilder peticion) {
        return peticion.header(HttpHeaders.AUTHORIZATION, TokensDePrueba.bearer(ADMINISTRADOR, "ADMINISTRADOR"));
    }
}
