package co.edu.unimagdalena.samariantour.nucleo.indicadores.infraestructura.entrada.rest;

import co.edu.unimagdalena.samariantour.nucleo.indicadores.dominio.modelo.AlertaSesgo;
import co.edu.unimagdalena.samariantour.nucleo.indicadores.dominio.puerto.entrada.ConsultarAlertasSesgoUseCase;
import co.edu.unimagdalena.samariantour.seguridadjwt.TokensDePrueba;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** El aviso de sesgo en el panel del administrador (RET-02, DN-43). */
@WebMvcTest(AlertasSesgoController.class)
@ActiveProfiles("demo")
class AlertasSesgoControllerTest {

    private static final UUID MAR_Y_SELVA = UUID.fromString("10000000-0000-0000-0000-000000000001");
    private static final Instant AYER = Instant.parse("2026-11-01T14:00:00Z");
    private static final Instant HOY = Instant.parse("2026-11-02T14:00:00Z");

    @Autowired
    private MockMvc http;

    @MockitoBean
    private ConsultarAlertasSesgoUseCase alertas;

    @Test
    @DisplayName("el administrador ve el índice, los dos porcentajes y desde cuándo está abierta")
    void abiertas() throws Exception {
        when(alertas.abiertas()).thenReturn(List.of(new AlertaSesgo(UUID.randomUUID(), MAR_Y_SELVA,
                new BigDecimal("3.00"), new BigDecimal("30.00"), new BigDecimal("10.00"), 450, 300, AYER, HOY, null)));

        http.perform(get("/admin/alertas-sesgo").header(HttpHeaders.AUTHORIZATION,
                        TokensDePrueba.bearer(UUID.randomUUID(), "ADMINISTRADOR")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].prestadorId").value(MAR_Y_SELVA.toString()))
                .andExpect(jsonPath("$[0].indice").value(3.0))
                .andExpect(jsonPath("$[0].porcentajeOfertas").value(10.0))
                .andExpect(jsonPath("$[0].abiertaEn").value(AYER.toString()))
                .andExpect(jsonPath("$[0].id").doesNotExist());
    }

    @Test
    @DisplayName("solo el administrador: el gestor del destino no ve las alertas")
    void soloElAdministrador() throws Exception {
        http.perform(get("/admin/alertas-sesgo").header(HttpHeaders.AUTHORIZATION,
                        TokensDePrueba.bearer(UUID.randomUUID(), "GESTOR_DESTINO")))
                .andExpect(status().isForbidden());
        verifyNoInteractions(alertas);
    }
}
