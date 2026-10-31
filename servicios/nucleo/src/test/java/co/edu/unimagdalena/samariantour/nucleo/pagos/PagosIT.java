package co.edu.unimagdalena.samariantour.nucleo.pagos;

import co.edu.unimagdalena.samariantour.nucleo.IntegracionPostgresBase;
import co.edu.unimagdalena.samariantour.nucleo.pagos.dominio.puerto.entrada.EjecutarDispersionesUseCase;
import co.edu.unimagdalena.samariantour.nucleo.pagos.dominio.puerto.entrada.ProcesarEventoPagoUseCase;
import co.edu.unimagdalena.samariantour.nucleo.pagos.dominio.puerto.salida.CuentasCobroPort;
import co.edu.unimagdalena.samariantour.nucleo.pagos.infraestructura.salida.modulos.PrestadoresPagosAdapter;
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

    /** Un prestador cuya cuenta no se puede leer: la consulta lanza un error inesperado. */
    static final UUID PRESTADOR_ROTO = UUID.fromString("10000000-0000-0000-0000-0000000000ee");

    @TestConfiguration
    static class RelojDePrueba {
        @Bean
        @Primary
        Clock relojDePrueba() {
            return PagosDePrueba.reloj(AHORA_DEL_SERVICIO);
        }

        @Bean
        @Primary
        CuentasCobroPort cuentasConUnPrestadorRoto(PrestadoresPagosAdapter cuentas) {
            return prestadorId -> {
                if (PRESTADOR_ROTO.equals(prestadorId)) {
                    throw new IllegalStateException("La cuenta no se pudo leer");
                }
                return cuentas.cuentaVigente(prestadorId);
            };
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
    @DisplayName("el pago y la solicitud se confirman juntos: si falla una parte, no queda ninguna y se aplaza")
    void todoONada() throws Exception {
        UUID id = guardar(confirmada(MIERCOLES)).id();
        String referencia = referencia(pagar(id, UUID.randomUUID().toString()));
        webhook(PagosDePrueba.evento("T-1", referencia, PRECIO_CENTAVOS, "APPROVED", json));
        // Una dispersión que ya ocupa la solicitud hace fallar la última escritura del procesamiento.
        jdbc.update("""
                INSERT INTO dispersiones (id, solicitud_id, prestador_id, monto_bruto, porcentaje_comision, comision,
                                          costo_transaccional, monto_neto, estado, intentos, creada_en)
                VALUES (?, ?, ?, 1, 0, 0, 0, 1, 'EXITOSA', 1, now())""", UUID.randomUUID(), id, UUID.randomUUID());

        assertThat(eventos.procesarPendientes()).isEqualTo(1);

        assertThat(jdbc.queryForObject("SELECT estado FROM solicitudes", String.class)).isEqualTo("CONFIRMADA");
        assertThat(jdbc.queryForObject("SELECT estado FROM intentos_pago", String.class)).isEqualTo("EN_CURSO");
        assertThat(jdbc.queryForObject("SELECT procesamiento || ' ' || intentos_procesamiento FROM eventos_pasarela",
                String.class)).isEqualTo("PENDIENTE 1");
        assertThat(jdbc.queryForObject("SELECT ultimo_error FROM eventos_pasarela", String.class))
                .startsWith("DataIntegrityViolationException");
        assertThat(contar("bandeja_salida")).isZero();
    }

    @Test
    @DisplayName("un evento que siempre falla no bloquea a los que llegan después; se aplaza con espera creciente")
    void eventoQueFallaNoBloquea() throws Exception {
        UUID rota = guardar(confirmada(MIERCOLES)).id();
        UUID sana = guardar(confirmada(MIERCOLES.plusDays(1))).id();
        String referenciaRota = referencia(pagar(rota, UUID.randomUUID().toString()));
        String referenciaSana = referencia(pagar(sana, UUID.randomUUID().toString()));
        ocuparDispersion(rota);
        Instant inicio = AHORA_DEL_SERVICIO.get();
        webhook(PagosDePrueba.evento("T-1", referenciaRota, PRECIO_CENTAVOS, "APPROVED", json));
        AHORA_DEL_SERVICIO.set(inicio.plusSeconds(1));
        webhook(PagosDePrueba.evento("T-2", referenciaSana, PRECIO_CENTAVOS, "APPROVED", json));

        assertThat(eventos.procesarPendientes()).isEqualTo(2);

        assertThat(estadoDe(sana)).isEqualTo("PAGADA");
        assertThat(estadoDe(rota)).isEqualTo("CONFIRMADA");
        assertThat(jdbc.queryForObject("SELECT siguiente_intento_en FROM eventos_pasarela WHERE transaccion_id = 'T-1'",
                Instant.class)).isEqualTo(inicio.plusSeconds(1 + 5));
        assertThat(eventos.procesarPendientes()).isZero();
    }

    @Test
    @DisplayName("se reintenta a los 5, 10, 20 y 40 s; al quinto fallo queda FALLIDO y se avisa al administrador")
    void eventoFallido() throws Exception {
        UUID id = guardar(confirmada(MIERCOLES)).id();
        String referencia = referencia(pagar(id, UUID.randomUUID().toString()));
        ocuparDispersion(id);
        webhook(PagosDePrueba.evento("T-1", referencia, PRECIO_CENTAVOS, "APPROVED", json));
        eventos.procesarPendientes();

        for (int fallo = 1; fallo <= 4; fallo++) {
            Instant siguiente = jdbc.queryForObject("SELECT siguiente_intento_en FROM eventos_pasarela", Instant.class);
            AHORA_DEL_SERVICIO.set(siguiente.minusMillis(1));
            assertThat(eventos.procesarPendientes()).isZero();
            AHORA_DEL_SERVICIO.set(siguiente);
            Instant antes = AHORA_DEL_SERVICIO.get();
            assertThat(eventos.procesarPendientes()).isEqualTo(1);
            if (fallo < 4) {
                assertThat(jdbc.queryForObject("SELECT siguiente_intento_en FROM eventos_pasarela", Instant.class))
                        .isEqualTo(antes.plus(Duration.ofSeconds(5).multipliedBy(1L << fallo)));
            }
        }

        assertThat(jdbc.queryForObject("SELECT procesamiento || ' ' || intentos_procesamiento FROM eventos_pasarela",
                String.class)).isEqualTo("FALLIDO 5");
        AHORA_DEL_SERVICIO.set(AHORA_DEL_SERVICIO.get().plus(Duration.ofHours(1)));
        assertThat(eventos.procesarPendientes()).isZero();
        assertThat(dispersiones.enviarAvisos()).isEqualTo(1);
        assertThat(dispersiones.enviarAvisos()).isZero();
        assertThat(jdbc.queryForObject("SELECT aviso_enviado_en IS NOT NULL FROM eventos_pasarela", Boolean.class))
                .isTrue();
        assertThat(estadoDe(id)).isEqualTo("CONFIRMADA");
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
                .andExpect(jsonPath("$[0].motivo").value("PAGO_TARDIO"))
                .andExpect(jsonPath("$[0].avisado").value(false));

        assertThat(dispersiones.enviarAvisos()).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT aviso_enviado_en IS NOT NULL FROM pagos_tardios", Boolean.class))
                .isTrue();
    }

    @Test
    @DisplayName("aprobado por otro monto: no paga la solicitud, queda para revisión con su motivo y se avisa (RF-49)")
    void montoDistinto() throws Exception {
        UUID id = guardar(confirmada(MIERCOLES)).id();
        String referencia = referencia(pagar(id, UUID.randomUUID().toString()));
        webhook(PagosDePrueba.evento("T-1", referencia, PRECIO_CENTAVOS - 100, "DECLINED", json));
        webhook(PagosDePrueba.evento("T-2", referencia, PRECIO_CENTAVOS - 100, "APPROVED", json));

        assertThat(eventos.procesarPendientes()).isEqualTo(2);

        assertThat(estadoDe(id)).isEqualTo("CONFIRMADA");
        assertThat(jdbc.queryForList("SELECT resultado FROM eventos_pasarela", String.class))
                .containsOnly("MONTO_DISTINTO");
        assertThat(jdbc.queryForObject("SELECT estado FROM intentos_pago", String.class)).isEqualTo("EN_CURSO");
        http.perform(comoAdministrador(get("/admin/pagos-tardios")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].transaccionId").value("T-2"))
                .andExpect(jsonPath("$[0].monto").value(299_999))
                .andExpect(jsonPath("$[0].motivo").value("MONTO_DISTINTO"));
        assertThat(dispersiones.enviarAvisos()).isEqualTo(1);
        assertThat(contar("dispersiones")).isZero();
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
    @DisplayName("un intento en curso aplaza el vencimiento hasta 30 min; se abandona al terminar la gracia (RF-55)")
    void abandonado() throws Exception {
        UUID id = guardar(confirmada(MIERCOLES)).id();
        AHORA_DEL_SERVICIO.set(FIN_VENTANA.minus(Duration.ofMinutes(5)));
        pagar(id, UUID.randomUUID().toString()).andExpect(status().isCreated());

        AHORA_DEL_SERVICIO.set(FIN_VENTANA.plus(Duration.ofMinutes(25)));
        assertThat(eventos.abandonarIntentos()).isZero();
        vencimientos.vencerLote();
        assertThat(jdbc.queryForObject("SELECT estado FROM solicitudes", String.class)).isEqualTo("CONFIRMADA");

        AHORA_DEL_SERVICIO.set(FIN_VENTANA.plus(Duration.ofMinutes(30)));
        assertThat(eventos.abandonarIntentos()).isEqualTo(1);
        vencimientos.vencerLote();

        assertThat(jdbc.queryForObject("SELECT estado FROM intentos_pago", String.class)).isEqualTo("ABANDONADO");
        assertThat(jdbc.queryForObject("SELECT estado FROM solicitudes", String.class)).isEqualTo("VENCIDA");
    }

    @Test
    @DisplayName("checkout 20 min antes del fin de la ventana: a los 30 min no se abandona y el aprobado paga (RF-55)")
    void aprobadoDentroDeLaGracia() throws Exception {
        UUID id = guardar(confirmada(MIERCOLES)).id();
        AHORA_DEL_SERVICIO.set(FIN_VENTANA.minus(Duration.ofMinutes(20)));
        String referencia = referencia(pagar(id, UUID.randomUUID().toString()));

        AHORA_DEL_SERVICIO.set(FIN_VENTANA.plus(Duration.ofMinutes(10)));
        assertThat(eventos.abandonarIntentos()).isZero();
        vencimientos.vencerLote();
        AHORA_DEL_SERVICIO.set(FIN_VENTANA.plus(Duration.ofMinutes(20)));
        webhook(PagosDePrueba.evento("T-1", referencia, PRECIO_CENTAVOS, "APPROVED", json));
        eventos.procesarPendientes();

        assertThat(estadoDe(id)).isEqualTo("PAGADA");
        assertThat(jdbc.queryForObject("SELECT resultado FROM eventos_pasarela", String.class)).isEqualTo("APLICADO");
        assertThat(contar("pagos_tardios")).isZero();
    }

    @Test
    @DisplayName("un intento que no se puede abandonar se aplaza un minuto y no detiene a los demás")
    void abandonoQueFalla() throws Exception {
        UUID id = guardar(confirmada(MIERCOLES)).id();
        pagar(id, UUID.randomUUID().toString()).andExpect(status().isCreated());
        // Un intento de una solicitud que ya no existe: liberarla falla.
        jdbc.update("""
                INSERT INTO intentos_pago (id, solicitud_id, turista_id, numero, referencia, monto_centavos, estado,
                                           creado_en, abandonar_en, revision)
                VALUES (?, ?, ?, 1, ?, 1, 'EN_CURSO', ?, ?, 0)""", UUID.randomUUID(), UUID.randomUUID(), TURISTA,
                "ST-huerfano", java.sql.Timestamp.from(AHORA), java.sql.Timestamp.from(AHORA));
        Instant ahora = AHORA_DEL_SERVICIO.get().plus(Duration.ofMinutes(30));
        AHORA_DEL_SERVICIO.set(ahora);

        assertThat(eventos.abandonarIntentos()).isEqualTo(2);

        assertThat(jdbc.queryForObject("SELECT estado FROM intentos_pago WHERE referencia <> 'ST-huerfano'",
                String.class)).isEqualTo("ABANDONADO");
        assertThat(jdbc.queryForObject("SELECT estado || ' ' || (abandonar_en = ?) FROM intentos_pago "
                + "WHERE referencia = 'ST-huerfano'", String.class, java.sql.Timestamp.from(ahora.plusSeconds(60))))
                .isEqualTo("EN_CURSO true");
        assertThat(eventos.abandonarIntentos()).isZero();
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
    @DisplayName("dos réplicas con un evento que falla: los demás se procesan una sola vez y ese se aplaza")
    void dosReplicasConUnoQueFalla() throws Exception {
        List<String> referencias = new ArrayList<>();
        UUID rota = null;
        for (int dia = 0; dia < 5; dia++) {
            UUID id = guardar(confirmada(MIERCOLES.plusDays(dia))).id();
            referencias.add(referencia(pagar(id, UUID.randomUUID().toString())));
            rota = id;
        }
        ocuparDispersion(rota);
        for (int i = 0; i < referencias.size(); i++) {
            webhook(PagosDePrueba.evento("T-" + i, referencias.get(i), PRECIO_CENTAVOS, "APPROVED", json));
        }

        aLaVez(() -> eventos.procesarPendientes());

        assertThat(jdbc.queryForObject("SELECT count(*) FROM eventos_pasarela WHERE resultado = 'APLICADO'",
                Integer.class)).isEqualTo(4);
        assertThat(contar("bandeja_salida")).isEqualTo(4);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM notificaciones_solicitud WHERE tipo = 'SOLICITUD_PAGADA'",
                Integer.class)).isEqualTo(8);
        assertThat(jdbc.queryForObject("SELECT procesamiento FROM eventos_pasarela WHERE transaccion_id = 'T-4'",
                String.class)).isEqualTo("PENDIENTE");
        assertThat(jdbc.queryForObject("SELECT intentos_procesamiento FROM eventos_pasarela "
                + "WHERE transaccion_id = 'T-4'", Integer.class)).isBetween(1, 2);
    }

    @Test
    @DisplayName("una dispersión con un error inesperado cuenta como fallo y no detiene a las demás (RF-54)")
    void dispersionConError() throws Exception {
        UUID id = guardar(confirmada(MIERCOLES)).id();
        String referencia = referencia(pagar(id, UUID.randomUUID().toString()));
        webhook(PagosDePrueba.evento("T-1", referencia, PRECIO_CENTAVOS, "APPROVED", json));
        eventos.procesarPendientes();
        jdbc.update("""
                INSERT INTO dispersiones (id, solicitud_id, prestador_id, monto_bruto, porcentaje_comision, comision,
                                          costo_transaccional, monto_neto, estado, intentos, creada_en,
                                          proximo_intento)
                VALUES (?, ?, ?, 1, 0, 0, 0, 1, 'PENDIENTE', 0, ?, ?)""", UUID.randomUUID(), UUID.randomUUID(),
                PRESTADOR_ROTO, java.sql.Timestamp.from(AHORA), java.sql.Timestamp.from(AHORA));

        assertThat(dispersiones.ejecutarPendientes()).isEqualTo(2);

        assertThat(jdbc.queryForObject("SELECT estado FROM dispersiones WHERE solicitud_id = ?", String.class, id))
                .isEqualTo("EXITOSA");
        assertThat(jdbc.queryForObject("SELECT estado || ' ' || intentos || ' ' || ultima_causa FROM dispersiones "
                + "WHERE prestador_id = ?", String.class, PRESTADOR_ROTO))
                .isEqualTo("PENDIENTE 1 Error inesperado: IllegalStateException");
        assertThat(dispersiones.ejecutarPendientes()).isZero();
    }

    @Test
    @DisplayName("los avisos que no se pueden enviar pasan al final de la fila y no detienen a los demás")
    void avisosSinBloqueo() throws Exception {
        for (int i = 0; i < 10; i++) {
            fallidaSinAviso(UUID.randomUUID(), AHORA.plusSeconds(i));
        }
        fallidaSinAviso(RESTAURANTE, AHORA.plusSeconds(60));

        assertThat(dispersiones.enviarAvisos()).isZero();
        AHORA_DEL_SERVICIO.set(AHORA_DEL_SERVICIO.get().plusSeconds(15));
        assertThat(dispersiones.enviarAvisos()).isEqualTo(1);

        assertThat(jdbc.queryForObject("SELECT aviso_enviado_en IS NOT NULL FROM dispersiones WHERE prestador_id = ?",
                Boolean.class, RESTAURANTE)).isTrue();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM dispersiones WHERE aviso_intentado_en IS NOT NULL",
                Integer.class)).isEqualTo(10);
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

    /** Una dispersión que ya ocupa la solicitud: hace fallar la última escritura del procesamiento de su pago. */
    private void ocuparDispersion(UUID solicitudId) {
        jdbc.update("""
                INSERT INTO dispersiones (id, solicitud_id, prestador_id, monto_bruto, porcentaje_comision, comision,
                                          costo_transaccional, monto_neto, estado, intentos, creada_en)
                VALUES (?, ?, ?, 1, 0, 0, 0, 1, 'EXITOSA', 1, now())""", UUID.randomUUID(), solicitudId,
                UUID.randomUUID());
    }

    /** Una dispersión fallida, de una solicitud cualquiera, que todavía no se avisó a su prestador. */
    private void fallidaSinAviso(UUID prestadorId, Instant creada) {
        jdbc.update("""
                INSERT INTO dispersiones (id, solicitud_id, prestador_id, monto_bruto, porcentaje_comision, comision,
                                          costo_transaccional, monto_neto, estado, intentos, ultima_causa, creada_en)
                VALUES (?, ?, ?, 1, 0, 0, 0, 1, 'FALLIDA', 4, 'La entidad rechazó la cuenta', ?)""",
                UUID.randomUUID(), UUID.randomUUID(), prestadorId, java.sql.Timestamp.from(creada));
    }

    private String estadoDe(UUID solicitudId) {
        return jdbc.queryForObject("SELECT estado FROM solicitudes WHERE id = ?", String.class, solicitudId);
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
