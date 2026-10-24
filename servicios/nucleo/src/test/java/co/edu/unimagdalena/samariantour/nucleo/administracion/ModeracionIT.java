package co.edu.unimagdalena.samariantour.nucleo.administracion;

import co.edu.unimagdalena.samariantour.nucleo.CatalogoSimulado;
import co.edu.unimagdalena.samariantour.nucleo.IntegracionPostgresBase;
import co.edu.unimagdalena.samariantour.nucleo.bandeja.dominio.puerto.entrada.PublicarAvisosUseCase;
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

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * La moderación de punta a punta dentro del núcleo (DN-08): la decisión entra por HTTP, se verifica con un catálogo
 * simulado, se guarda en PostgreSQL y deja en la bandeja el aviso que espera el catálogo.
 */
@SpringBootTest(properties = "nucleo.bandeja.publicacion.cron=-")
@AutoConfigureMockMvc
@ActiveProfiles("demo")
class ModeracionIT extends IntegracionPostgresBase {

    private static final CatalogoSimulado CATALOGO = new CatalogoSimulado();
    private static final String LANCHA = "b00000000000000000000017";
    private static final String OPINION = "d0000000000000000000000b";

    @Autowired
    private MockMvc http;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private PublicarAvisosUseCase publicar;

    @DynamicPropertySource
    static void catalogoSimulado(DynamicPropertyRegistry registro) {
        registro.add("nucleo.catalogo.url", CATALOGO::url);
    }

    @BeforeEach
    void preparar() {
        jdbc.update("DELETE FROM decisiones_moderacion");
        jdbc.update("DELETE FROM bandeja_salida");
        CATALOGO.reiniciar();
        CATALOGO.responder(404);
        CATALOGO.responder("/internal/moderacion/ofertas/" + LANCHA, 200, """
                {"id": "%s", "tipo": "ACTIVIDAD", "nombre": "Paseo en lancha a Playa Blanca",
                 "prestadorId": "10000000-0000-0000-0000-000000000001", "numeroVersion": 1,
                 "enviadaEn": "2026-10-01T13:00:00Z"}""".formatted(LANCHA));
        CATALOGO.responder("/internal/moderacion/opiniones/" + OPINION, 200, """
                {"id": "%s", "ofertaId": "b00000000000000000000006",
                 "turistaId": "80000000-0000-0000-0000-000000000008", "estado": "EN_MODERACION",
                 "fecha": "2026-10-01T13:00:00Z"}""".formatted(OPINION));
    }

    @AfterAll
    static void apagar() {
        CATALOGO.close();
    }

    @Test
    @DisplayName("aprobar una oferta guarda la decisión y deja en la bandeja el aviso MODERACION del contrato")
    void aprobarOferta() throws Exception {
        decidirOferta("{\"numeroVersion\": 1, \"decision\": \"APROBAR\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.administradorId").value("60000000-0000-0000-0000-000000000006"));

        Map<String, Object> decision = jdbc.queryForMap("SELECT * FROM decisiones_moderacion");
        assertThat(decision).containsEntry("tipo", "OFERTA").containsEntry("moderado_id", LANCHA)
                .containsEntry("numero_version", 1).containsEntry("decision", "APROBAR");
        Map<String, Object> aviso = jdbc.queryForMap("SELECT id_aviso, destino, cuerpo::text AS cuerpo "
                + "FROM bandeja_salida");
        assertThat(aviso).containsEntry("id_aviso", "moderacion-oferta-" + LANCHA + "-v1")
                .containsEntry("destino", "OFERTAS");
        assertThat((String) aviso.get("cuerpo")).contains("\"tipo\": \"MODERACION\"")
                .contains("\"ofertaId\": \"" + LANCHA + "\"").contains("\"numeroVersion\": 1")
                .contains("\"decision\": \"APROBAR\"").doesNotContain("motivo");

        // La bandeja lo publica en la entrada de ofertas del catálogo.
        publicar.publicarLote();
        assertThat(CATALOGO.recibidas()).extracting(CatalogoSimulado.Peticion::ruta).contains("/internal/avisos");
    }

    @Test
    @DisplayName("repetir la misma decisión responde 200 con la de entonces y no encola otro aviso; la contraria, 409")
    void repetida() throws Exception {
        decidirOferta("{\"numeroVersion\": 1, \"decision\": \"RECHAZAR\", \"motivo\": \"Sin fotos\"}")
                .andExpect(status().isOk());
        String primera = jdbc.queryForObject("SELECT decidida_en::text FROM decisiones_moderacion", String.class);
        CATALOGO.responder("/internal/moderacion/ofertas/" + LANCHA, 404, "{}");

        decidirOferta("{\"numeroVersion\": 1, \"decision\": \"RECHAZAR\", \"motivo\": \"Otro motivo\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.motivo").value("Sin fotos"));
        decidirOferta("{\"numeroVersion\": 1, \"decision\": \"APROBAR\"}")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("MODERACION_YA_DECIDIDA"));

        assertThat(jdbc.queryForObject("SELECT decidida_en::text FROM decisiones_moderacion", String.class))
                .isEqualTo(primera);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM bandeja_salida", Integer.class)).isEqualTo(1);
    }

    @Test
    @DisplayName("una versión que ya no es la de moderación responde 409; con el catálogo caído, 503; sin guardar nada")
    void sinDecidir() throws Exception {
        decidirOferta("{\"numeroVersion\": 2, \"decision\": \"APROBAR\"}")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("VERSION_NO_VIGENTE"));

        CATALOGO.responder("/internal/moderacion/ofertas/" + LANCHA, 503, "{}");
        decidirOferta("{\"numeroVersion\": 1, \"decision\": \"APROBAR\"}")
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.codigo").value("CATALOGO_NO_DISPONIBLE"));

        assertThat(jdbc.queryForObject("SELECT count(*) FROM decisiones_moderacion", Integer.class)).isZero();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM bandeja_salida", Integer.class)).isZero();
    }

    @Test
    @DisplayName("rechazar una opinión deja el aviso MODERACION_OPINION, con su motivo, para la entrada de opiniones")
    void rechazarOpinion() throws Exception {
        http.perform(comoAdministrador(post("/admin/moderacion/opiniones/{id}/decision", OPINION))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"decision\": \"RECHAZAR\", \"motivo\": \"Lenguaje ofensivo\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tipo").value("OPINION"));

        Map<String, Object> aviso = jdbc.queryForMap("SELECT id_aviso, destino, cuerpo::text AS cuerpo "
                + "FROM bandeja_salida");
        assertThat(aviso).containsEntry("id_aviso", "moderacion-opinion-" + OPINION)
                .containsEntry("destino", "OPINIONES");
        assertThat((String) aviso.get("cuerpo")).contains("\"tipo\": \"MODERACION_OPINION\"")
                .contains("\"opinionId\": \"" + OPINION + "\"").contains("\"motivo\": \"Lenguaje ofensivo\"")
                .doesNotContain("numeroVersion");
    }

    private ResultActions decidirOferta(String cuerpo) throws Exception {
        return http.perform(comoAdministrador(post("/admin/moderacion/ofertas/{id}/decision", LANCHA))
                .contentType(MediaType.APPLICATION_JSON).content(cuerpo));
    }

    private static MockHttpServletRequestBuilder comoAdministrador(MockHttpServletRequestBuilder peticion) {
        return peticion.header("X-Usuario-Id", "60000000-0000-0000-0000-000000000006")
                .header("X-Rol", "ADMINISTRADOR");
    }
}
