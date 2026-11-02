package co.edu.unimagdalena.samariantour.nucleo.indicadores.infraestructura.entrada.rest;

import co.edu.unimagdalena.samariantour.nucleo.indicadores.dominio.modelo.OrigenExposicion;
import co.edu.unimagdalena.samariantour.nucleo.indicadores.dominio.modelo.RegistroExposicion;
import co.edu.unimagdalena.samariantour.nucleo.indicadores.dominio.puerto.entrada.RegistrarExposicionUseCase;
import co.edu.unimagdalena.samariantour.seguridadjwt.ServiciosInternos;
import co.edu.unimagdalena.samariantour.seguridadjwt.TokensDePrueba;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** El registro de exposición que envía la recomendación con su token de servicio (RET-02, DN-38, DN-42). */
@WebMvcTest(ExposicionController.class)
@ActiveProfiles("demo")
class ExposicionControllerTest {

    private static final String COMO_RECOMENDACION =
            TokensDePrueba.bearerServicio(ServiciosInternos.RECOMENDACION, ServiciosInternos.NUCLEO);
    private static final String EXPOSICION = """
            {"fecha": "2026-11-02T15:00:00Z", "origen": "RECOMENDACION", "motor": "REGLAS",
             "ofertas": [{"ofertaId": "b1", "prestadorId": "10000000-0000-0000-0000-000000000001", "posicion": 1}]}""";

    @Autowired
    private MockMvc http;

    @MockitoBean
    private RegistrarExposicionUseCase exposicion;

    @Test
    @DisplayName("la recomendación la envía con su token y queda registrada: 202")
    void registrar() throws Exception {
        enviar(COMO_RECOMENDACION, EXPOSICION).andExpect(status().isAccepted());

        ArgumentCaptor<RegistroExposicion> registro = ArgumentCaptor.forClass(RegistroExposicion.class);
        verify(exposicion).registrar(registro.capture());
        assertThat(registro.getValue().fecha()).isEqualTo(Instant.parse("2026-11-02T15:00:00Z"));
        assertThat(registro.getValue().origen()).isEqualTo(OrigenExposicion.RECOMENDACION);
        assertThat(registro.getValue().ofertas()).singleElement().satisfies(oferta ->
                assertThat(oferta.prestadorId()).isEqualTo(UUID.fromString("10000000-0000-0000-0000-000000000001")));
    }

    @Test
    @DisplayName("un origen desconocido o una oferta sin posición son 400 DATO_INVALIDO")
    void invalida() throws Exception {
        enviar(COMO_RECOMENDACION, EXPOSICION.replace("RECOMENDACION", "OTRO"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("DATO_INVALIDO"));
        enviar(COMO_RECOMENDACION, EXPOSICION.replace(", \"posicion\": 1", ""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("posicion: empieza en 1"));
        verifyNoInteractions(exposicion);
    }

    @Test
    @DisplayName("solo la recomendación: otro servicio, un usuario (aun el administrador) o otra audiencia son 401")
    void soloLaRecomendacion() throws Exception {
        enviar(TokensDePrueba.bearerServicio(ServiciosInternos.NUCLEO, ServiciosInternos.NUCLEO), EXPOSICION)
                .andExpect(status().isUnauthorized());
        enviar(TokensDePrueba.bearer(UUID.randomUUID(), "ADMINISTRADOR"), EXPOSICION)
                .andExpect(status().isUnauthorized());
        enviar(TokensDePrueba.bearerServicio(ServiciosInternos.RECOMENDACION, ServiciosInternos.CATALOGO),
                EXPOSICION).andExpect(status().isUnauthorized());
        verifyNoInteractions(exposicion);
    }

    private ResultActions enviar(String token, String cuerpo) throws Exception {
        return http.perform(post("/internal/exposiciones").header(HttpHeaders.AUTHORIZATION, token)
                .contentType(MediaType.APPLICATION_JSON).content(cuerpo));
    }
}
