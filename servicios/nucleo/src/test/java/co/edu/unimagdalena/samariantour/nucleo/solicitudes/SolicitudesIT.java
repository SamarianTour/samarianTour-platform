package co.edu.unimagdalena.samariantour.nucleo.solicitudes;

import co.edu.unimagdalena.samariantour.nucleo.CatalogoSimulado;
import co.edu.unimagdalena.samariantour.nucleo.IntegracionPostgresBase;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Las solicitudes de punta a punta (DN-09 a DN-16): por HTTP con los headers de la demo, contra PostgreSQL, un catálogo
 * simulado y la tabla de prestadores y contactos de {@code application-demo.yml}.
 */
@SpringBootTest(properties = {"nucleo.bandeja.publicacion.cron=-", "nucleo.solicitudes.vencimiento.cron=-",
        "nucleo.solicitudes.notificaciones.cron=-", "nucleo.solicitudes.idempotencia.cron=-"})
@AutoConfigureMockMvc
@ActiveProfiles("demo")
class SolicitudesIT extends IntegracionPostgresBase {

    private static final CatalogoSimulado CATALOGO = new CatalogoSimulado();
    private static final String LAURA = "90000000-0000-0000-0000-000000000001";
    private static final String CARLOS = "90000000-0000-0000-0000-000000000002";
    private static final String SIN_WHATSAPP = "90000000-0000-0000-0000-000000000009";
    private static final String OPERADOR_1 = "60000000-0000-0000-0000-000000000001";
    private static final String OPERADOR_2 = "60000000-0000-0000-0000-000000000002";
    private static final String CABO_SAN_JUAN = "b00000000000000000000001";
    private static final String CABANA = "b0000000000000000000000f";
    private static final String COCINA_CARIBE = "b00000000000000000000013";

    /** En 3 días, para cumplir la anticipación de 2 días calendario en la hora de Colombia (RF-30). */
    private final String fecha = LocalDate.now(ZoneId.of("America/Bogota")).plusDays(3).toString();

    @Autowired
    private MockMvc http;

    @Autowired
    private JdbcTemplate jdbc;

    @DynamicPropertySource
    static void catalogoSimulado(DynamicPropertyRegistry registro) {
        registro.add("nucleo.catalogo.url", CATALOGO::url);
    }

    @BeforeEach
    void preparar() {
        jdbc.update("DELETE FROM claves_idempotencia");
        jdbc.update("DELETE FROM notificaciones_solicitud");
        jdbc.update("DELETE FROM transiciones_solicitud");
        jdbc.update("DELETE FROM solicitudes");
        CATALOGO.reiniciar();
        CATALOGO.responder(404);
        CATALOGO.responder("/internal/ofertas/" + CABO_SAN_JUAN, 200, """
                {"id": "b00000000000000000000001", "tipo": "ACTIVIDAD", "nombre": "Caminata a Cabo San Juan del Guía",
                 "prestadorId": "10000000-0000-0000-0000-000000000001", "zonaId": "a00000000000000000000005",
                 "horariosSalida": ["07:00", "08:00"], "capacidadMaxima": 12}""");
    }

    @AfterAll
    static void apagar() {
        CATALOGO.close();
    }

    @Test
    @DisplayName("el recorrido: enviar, repetir con la misma clave, la cola del operador, atender y confirmar")
    void recorrido() throws Exception {
        String clave = UUID.randomUUID().toString();
        String creada = enviar(LAURA, clave, "07:00", 2).andExpect(status().isCreated())
                .andExpect(jsonPath("$.estado").value("PENDIENTE"))
                .andExpect(jsonPath("$.venceEn").exists())
                .andReturn().getResponse().getContentAsString();
        String id = JsonPath.read(creada, "$.id");

        // Repetir con la misma clave: la misma respuesta, sin crear otra.
        String repetida = enviar(LAURA, clave, "07:00", 2).andExpect(status().isCreated())
                .andExpect(header().string("Idempotency-Replayed", "true"))
                .andReturn().getResponse().getContentAsString();
        assertThat(repetida).isEqualTo(creada);
        assertThat(contar("solicitudes")).isEqualTo(1);
        // La misma clave con otro cuerpo: 422.
        enviar(LAURA, clave, "07:00", 3).andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.codigo").value("CLAVE_IDEMPOTENCIA_REUTILIZADA"));
        // Otra clave con lo mismo: duplicada (RF-31).
        enviar(LAURA, UUID.randomUUID().toString(), "07:00", 2).andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("SOLICITUD_DUPLICADA"));

        http.perform(comoOperador(get("/operador/solicitudes"), OPERADOR_1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contenido[0].id").value(id))
                .andExpect(jsonPath("$.contenido[0].proximaAVencer").value(false));
        http.perform(comoOperador(get("/operador/avisos"), OPERADOR_1))
                .andExpect(jsonPath("$.contenido[0].tipo").value("NUEVA_SOLICITUD"))
                .andExpect(jsonPath("$.contenido[0].solicitudId").value(id));
        http.perform(comoOperador(get("/operador/solicitudes/{id}", id), OPERADOR_1))
                .andExpect(jsonPath("$.whatsappPrestador").value("+573001110101"));

        http.perform(comoOperador(post("/operador/solicitudes/{id}/atencion", id), OPERADOR_1)
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {"resultado": "ATENDIDA", "precioBase": 300000, "aparteAcordado": true,
                                 "politicaCancelacion": "Sin reembolso a menos de 24 horas del servicio"}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("ATENDIDA"));

        http.perform(comoTurista(get("/turista/solicitudes/{id}", id), LAURA))
                .andExpect(jsonPath("$.estado").value("ATENDIDA"))
                .andExpect(jsonPath("$.condiciones.precioBase").value(300000))
                .andExpect(jsonPath("$.condiciones.aparteHasta").isNotEmpty())
                .andExpect(jsonPath("$.historial.length()").value(2));

        String confirmacion = UUID.randomUUID().toString();
        responder(id, LAURA, confirmacion, "CONFIRMAR").andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("CONFIRMADA"));
        responder(id, LAURA, confirmacion, "CONFIRMAR").andExpect(status().isOk())
                .andExpect(header().string("Idempotency-Replayed", "true"));
        responder(id, LAURA, UUID.randomUUID().toString(), "DECLINAR").andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("TRANSICION_INVALIDA"));

        assertThat(jdbc.queryForList("SELECT tipo FROM notificaciones_solicitud ORDER BY creada_en, tipo",
                String.class)).containsExactly("NUEVA_SOLICITUD", "SOLICITUD_RECIBIDA", "SOLICITUD_ATENDIDA",
                "SOLICITUD_CONFIRMADA");
    }

    @Test
    @DisplayName("no disponible; y la solicitud ajena responde 404, para otro turista y para otro operador")
    void noDisponibleYAjenas() throws Exception {
        String id = JsonPath.read(enviar(LAURA, UUID.randomUUID().toString(), "08:00", 4)
                .andReturn().getResponse().getContentAsString(), "$.id");

        http.perform(comoTurista(get("/turista/solicitudes/{id}", id), CARLOS)).andExpect(status().isNotFound());
        http.perform(comoOperador(get("/operador/solicitudes/{id}", id), OPERADOR_2)).andExpect(status().isNotFound());
        http.perform(comoOperador(post("/operador/solicitudes/{id}/atencion", id), OPERADOR_2)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"resultado\": \"NO_DISPONIBLE\"}"))
                .andExpect(status().isNotFound());

        http.perform(comoOperador(post("/operador/solicitudes/{id}/atencion", id), OPERADOR_1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"resultado\": \"NO_DISPONIBLE\", \"nota\": \"Sin cupo esa semana\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("NO_DISPONIBLE"))
                .andExpect(jsonPath("$.venceEn").doesNotExist());
        http.perform(comoOperador(get("/operador/solicitudes").param("estado", "NO_DISPONIBLE"), OPERADOR_1))
                .andExpect(jsonPath("$.contenido[0].id").value(id));
    }

    @Test
    @DisplayName("catálogo caído: 503 sin crear nada; al volver, el reintento con la misma clave la crea (ADR-04)")
    void catalogoCaido() throws Exception {
        String clave = UUID.randomUUID().toString();
        CATALOGO.responder("/internal/ofertas/" + CABO_SAN_JUAN, 503, "{}");

        enviar(LAURA, clave, "07:00", 2).andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.codigo").value("CATALOGO_NO_DISPONIBLE"));
        assertThat(contar("solicitudes")).isZero();
        assertThat(contar("claves_idempotencia")).isZero();

        preparar();
        enviar(LAURA, clave, "07:00", 2).andExpect(status().isCreated())
                .andExpect(header().doesNotExist("Idempotency-Replayed"));
        assertThat(contar("solicitudes")).isEqualTo(1);
    }

    @Test
    @DisplayName("oferta no visible 404; turista sin WhatsApp 409; fecha muy cercana 400; sin la clave 400")
    void rechazos() throws Exception {
        http.perform(comoTurista(post("/turista/solicitudes"), LAURA).header("Idempotency-Key", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {"ofertaId": "b000000000000000000000ff", "fechaInicio": "%s", "personas": 2}"""
                                .formatted(fecha)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.codigo").value("OFERTA_NO_ENCONTRADA"));
        enviar(SIN_WHATSAPP, UUID.randomUUID().toString(), "07:00", 2).andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("WHATSAPP_NO_VERIFICADO"));
        http.perform(comoTurista(post("/turista/solicitudes"), LAURA).header("Idempotency-Key", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {"ofertaId": "%s", "fechaInicio": "%s", "horaSalida": "07:00", "personas": 2}"""
                                .formatted(CABO_SAN_JUAN, LocalDate.now(ZoneId.of("America/Bogota")).plusDays(1))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.startsWith("fechaInicio:")));
        http.perform(comoTurista(post("/turista/solicitudes"), LAURA).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"ofertaId\": \"%s\"}".formatted(CABO_SAN_JUAN)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("DATO_INVALIDO"));
        assertThat(contar("solicitudes")).isZero();
    }

    @Test
    @DisplayName("alojamiento y restaurante con sus campos (RF-30): los atiende el operador 2; un campo ajeno es 400")
    void alojamientoYRestaurante() throws Exception {
        CATALOGO.responder("/internal/ofertas/" + CABANA, 200, """
                {"id": "b0000000000000000000000f", "tipo": "ALOJAMIENTO", "nombre": "Cabaña frente al mar en Buritaca",
                 "prestadorId": "30000000-0000-0000-0000-000000000003", "zonaId": "a00000000000000000000006",
                 "capacidadMaxima": 4}""");
        CATALOGO.responder("/internal/ofertas/" + COCINA_CARIBE, 200, """
                {"id": "b00000000000000000000013", "tipo": "RESTAURANTE",
                 "nombre": "Cocina del Caribe en el Centro Histórico",
                 "prestadorId": "40000000-0000-0000-0000-000000000004", "zonaId": "a00000000000000000000001",
                 "capacidadMaxima": 12}""");
        String salida = LocalDate.parse(fecha).plusDays(2).toString();

        enviarCuerpo(LAURA, """
                {"ofertaId": "%s", "fechaInicio": "%s", "fechaSalida": "%s", "numeroUnidades": 1, "personas": 3}"""
                .formatted(CABANA, fecha, salida))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.oferta.tipo").value("ALOJAMIENTO"))
                .andExpect(jsonPath("$.fechaSalida").value(salida))
                .andExpect(jsonPath("$.numeroUnidades").value(1))
                .andExpect(jsonPath("$.horaSalida").doesNotExist());
        enviarCuerpo(LAURA, """
                {"ofertaId": "%s", "fechaInicio": "%s", "horaReserva": "19:30", "personas": 4}"""
                .formatted(COCINA_CARIBE, fecha))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.horaReserva").value("19:30"));

        http.perform(comoOperador(get("/operador/solicitudes"), OPERADOR_2))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElementos").value(2))
                .andExpect(jsonPath("$.contenido[0].fechaSalida").value(salida))
                .andExpect(jsonPath("$.contenido[1].horaReserva").value("19:30"));
        enviarCuerpo(LAURA, """
                {"ofertaId": "%s", "fechaInicio": "%s", "horaReserva": "19:30", "personas": 2}"""
                .formatted(COCINA_CARIBE, fecha))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("SOLICITUD_DUPLICADA"));
        enviarCuerpo(CARLOS, """
                {"ofertaId": "%s", "fechaInicio": "%s", "horaSalida": "19:30", "personas": 2}"""
                .formatted(COCINA_CARIBE, fecha))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("DATO_INVALIDO"))
                .andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.startsWith("horaSalida:")));
        enviarCuerpo(CARLOS, """
                {"ofertaId": "%s", "fechaInicio": "%s", "numeroUnidades": 1, "personas": 2}"""
                .formatted(CABANA, fecha))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.startsWith("fechaSalida:")));
        assertThat(contar("solicitudes")).isEqualTo(2);
    }

    private ResultActions enviarCuerpo(String turista, String cuerpo) throws Exception {
        return http.perform(comoTurista(post("/turista/solicitudes"), turista)
                .header("Idempotency-Key", UUID.randomUUID().toString())
                .contentType(MediaType.APPLICATION_JSON).content(cuerpo));
    }

    private ResultActions enviar(String turista, String clave, String hora, int personas) throws Exception {
        return http.perform(comoTurista(post("/turista/solicitudes"), turista).header("Idempotency-Key", clave)
                .contentType(MediaType.APPLICATION_JSON).content("""
                        {"ofertaId": "%s", "fechaInicio": "%s", "horaSalida": "%s", "personas": %d,
                         "nota": "Vamos con una niña"}""".formatted(CABO_SAN_JUAN, fecha, hora, personas)));
    }

    private ResultActions responder(String id, String turista, String clave, String decision) throws Exception {
        return http.perform(comoTurista(post("/turista/solicitudes/{id}/respuesta", id), turista)
                .header("Idempotency-Key", clave).contentType(MediaType.APPLICATION_JSON)
                .content("{\"decision\": \"%s\"}".formatted(decision)));
    }

    private int contar(String tabla) {
        return jdbc.queryForObject("SELECT count(*) FROM " + tabla, Integer.class);
    }

    private static MockHttpServletRequestBuilder comoTurista(MockHttpServletRequestBuilder peticion, String id) {
        return peticion.header("X-Usuario-Id", id).header("X-Rol", "TURISTA");
    }

    private static MockHttpServletRequestBuilder comoOperador(MockHttpServletRequestBuilder peticion, String id) {
        return peticion.header("X-Usuario-Id", id).header("X-Rol", "OPERADOR_DISPONIBILIDAD");
    }
}
