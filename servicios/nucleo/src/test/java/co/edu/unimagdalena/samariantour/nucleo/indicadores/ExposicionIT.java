package co.edu.unimagdalena.samariantour.nucleo.indicadores;

import co.edu.unimagdalena.samariantour.nucleo.CatalogoSimulado;
import co.edu.unimagdalena.samariantour.nucleo.IntegracionPostgresBase;
import co.edu.unimagdalena.samariantour.nucleo.indicadores.dominio.modelo.OfertaExpuesta;
import co.edu.unimagdalena.samariantour.nucleo.indicadores.dominio.modelo.OrigenExposicion;
import co.edu.unimagdalena.samariantour.nucleo.indicadores.dominio.modelo.RegistroExposicion;
import co.edu.unimagdalena.samariantour.nucleo.indicadores.dominio.modelo.RevisionSesgo;
import co.edu.unimagdalena.samariantour.nucleo.indicadores.dominio.puerto.entrada.EvaluarIndiceExposicionUseCase;
import co.edu.unimagdalena.samariantour.nucleo.indicadores.dominio.puerto.entrada.RegistrarExposicionUseCase;
import co.edu.unimagdalena.samariantour.seguridadjwt.ServiciosInternos;
import co.edu.unimagdalena.samariantour.seguridadjwt.TokensDePrueba;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * La exposición y el sesgo de punta a punta (RET-02, DN-42, DN-43): la recomendación envía por HTTP con su token, la
 * revisión pide las ofertas elegibles a un catálogo simulado y el administrador ve la alerta en su panel.
 */
@SpringBootTest(properties = "nucleo.bandeja.publicacion.cron=-")
@AutoConfigureMockMvc
@ActiveProfiles("demo")
class ExposicionIT extends IntegracionPostgresBase {

    private static final CatalogoSimulado CATALOGO = new CatalogoSimulado();
    private static final UUID MAR_Y_SELVA = UUID.fromString("10000000-0000-0000-0000-000000000001");
    private static final UUID KAYAK = UUID.fromString("20000000-0000-0000-0000-000000000002");

    @Autowired
    private MockMvc http;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private RegistrarExposicionUseCase registrar;

    @Autowired
    private EvaluarIndiceExposicionUseCase revision;

    @DynamicPropertySource
    static void catalogoSimulado(DynamicPropertyRegistry registro) {
        registro.add("nucleo.catalogo.url", CATALOGO::url);
    }

    @BeforeEach
    void preparar() {
        jdbc.update("DELETE FROM exposiciones");
        jdbc.update("DELETE FROM alertas_sesgo");
        jdbc.update("DELETE FROM revisiones_sesgo");
        CATALOGO.reiniciar();
        CATALOGO.responder(404);
        CATALOGO.responder("/internal/ofertas/por-prestador", 200, """
                [{"prestadorId": "%s", "ofertas": 10}, {"prestadorId": "%s", "ofertas": 90}]"""
                .formatted(MAR_Y_SELVA, KAYAK));
    }

    @AfterAll
    static void apagar() {
        CATALOGO.close();
    }

    @Test
    @DisplayName("la recomendación envía lo que mostró y queda una fila por oferta, sin datos del turista")
    void registrar() throws Exception {
        http.perform(post("/internal/exposiciones")
                        .header(HttpHeaders.AUTHORIZATION, TokensDePrueba.bearerServicio(
                                ServiciosInternos.RECOMENDACION, ServiciosInternos.NUCLEO))
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {"fecha": "%s", "origen": "RECOMENDACION", "motor": "IA",
                                 "ofertas": [{"ofertaId": "b1", "prestadorId": "%s", "posicion": 1},
                                             {"ofertaId": "b6", "prestadorId": "%s", "posicion": 2}]}"""
                                .formatted(Instant.now(), MAR_Y_SELVA, KAYAK)))
                .andExpect(status().isAccepted());

        assertThat(jdbc.queryForList("SELECT prestador_id FROM exposiciones ORDER BY posicion", UUID.class))
                .containsExactly(MAR_Y_SELVA, KAYAK);
        assertThat(jdbc.queryForObject("SELECT COUNT(DISTINCT consulta_id) FROM exposiciones", Long.class))
                .isEqualTo(1);
    }

    @Test
    @DisplayName("con 200 recomendaciones, quien sale en la mitad con el 10 % de las ofertas tiene alerta en el panel")
    void alertaEnElPanel() throws Exception {
        Instant ahora = Instant.now();
        for (int i = 0; i < 200; i++) {
            registrar.registrar(recomendacion(ahora.minusSeconds(i)));
        }
        registrar.registrar(recomendacion(ahora.minus(Duration.ofDays(100))));

        RevisionSesgo resultado = revision.evaluar();

        assertThat(resultado.resultado()).isEqualTo(RevisionSesgo.Resultado.EVALUADA);
        assertThat(resultado.abiertas()).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM exposiciones", Long.class)).isEqualTo(400);
        http.perform(get("/admin/alertas-sesgo")
                        .header(HttpHeaders.AUTHORIZATION, TokensDePrueba.bearer(UUID.randomUUID(), "ADMINISTRADOR")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].prestadorId").value(MAR_Y_SELVA.toString()))
                .andExpect(jsonPath("$[0].indice").value(5.0))
                .andExpect(jsonPath("$[0].recomendaciones").value(200));
        assertThat(revision.evaluar().resultado()).isEqualTo(RevisionSesgo.Resultado.YA_REVISADA);
    }

    @Test
    @DisplayName("si el catálogo no responde, el día no queda revisado y la próxima hora se intenta de nuevo")
    void catalogoCaido() {
        CATALOGO.reiniciar();
        CATALOGO.responder(503);

        assertThat(revision.evaluar().resultado()).isEqualTo(RevisionSesgo.Resultado.CATALOGO_NO_DISPONIBLE);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM revisiones_sesgo", Long.class)).isZero();
    }

    private static RegistroExposicion recomendacion(Instant fecha) {
        return new RegistroExposicion(UUID.randomUUID(), fecha, OrigenExposicion.RECOMENDACION, "REGLAS", List.of(
                new OfertaExpuesta("b1", MAR_Y_SELVA, 1), new OfertaExpuesta("b6", KAYAK, 2)));
    }
}
