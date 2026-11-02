package co.edu.unimagdalena.samariantour.nucleo.indicadores.infraestructura.entrada.rest;

import co.edu.unimagdalena.samariantour.nucleo.indicadores.dominio.excepcion.DatoInvalidoException;
import co.edu.unimagdalena.samariantour.nucleo.indicadores.dominio.modelo.ConteoCategoria;
import co.edu.unimagdalena.samariantour.nucleo.indicadores.dominio.modelo.ConteoDemanda;
import co.edu.unimagdalena.samariantour.nucleo.indicadores.dominio.modelo.ConteoEstado;
import co.edu.unimagdalena.samariantour.nucleo.indicadores.dominio.modelo.DistribucionCategoria;
import co.edu.unimagdalena.samariantour.nucleo.indicadores.dominio.modelo.EmbudoConversion;
import co.edu.unimagdalena.samariantour.nucleo.indicadores.dominio.modelo.Periodo;
import co.edu.unimagdalena.samariantour.nucleo.indicadores.dominio.modelo.Tablero;
import co.edu.unimagdalena.samariantour.nucleo.indicadores.dominio.puerto.entrada.ConsultarDistribucionCategoriaUseCase;
import co.edu.unimagdalena.samariantour.nucleo.indicadores.dominio.puerto.entrada.ConsultarEmbudoUseCase;
import co.edu.unimagdalena.samariantour.nucleo.indicadores.dominio.puerto.entrada.ConsultarTableroUseCase;
import co.edu.unimagdalena.samariantour.seguridadjwt.TokensDePrueba;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Las rutas de los indicadores del gestor del destino (RF-85 a RF-89, DN-41). */
@WebMvcTest(IndicadoresController.class)
@ActiveProfiles("demo")
class IndicadoresControllerTest {

    private static final UUID GESTOR = UUID.fromString("60000000-0000-0000-0000-000000000003");
    private static final LocalDate DESDE = LocalDate.parse("2026-10-01");
    private static final LocalDate HASTA = LocalDate.parse("2026-10-31");
    private static final Periodo OCTUBRE = new Periodo(DESDE, HASTA);

    @Autowired
    private MockMvc http;

    @MockitoBean
    private ConsultarTableroUseCase tablero;

    @MockitoBean
    private ConsultarEmbudoUseCase embudo;

    @MockitoBean
    private ConsultarDistribucionCategoriaUseCase distribucion;

    @Test
    @DisplayName("el tablero lee las fechas ISO y omite los conteos de 1 o 2")
    void tablero() throws Exception {
        when(tablero.consultarTablero(DESDE, HASTA)).thenReturn(Tablero.de(OCTUBRE, List.of(
                new ConteoDemanda("tayrona", "ACTIVIDAD", 18), new ConteoDemanda("minca", "ALOJAMIENTO", 2))));

        http.perform(comoGestor(get("/gestor/indicadores/tablero").param("desde", "2026-10-01")
                        .param("hasta", "2026-10-31")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.desde").value("2026-10-01"))
                .andExpect(jsonPath("$.total").value(20))
                .andExpect(jsonPath("$.filas[0].solicitudes").value(18))
                .andExpect(jsonPath("$.filas[1].zonaId").value("minca"))
                .andExpect(jsonPath("$.filas[1].solicitudes").doesNotExist());
    }

    @Test
    @DisplayName("el embudo trae las cuatro etapas con su porcentaje, y los cuatro tipos")
    void embudo() throws Exception {
        when(embudo.consultarEmbudo(null, null)).thenReturn(EmbudoConversion.de(OCTUBRE, List.of(
                new ConteoEstado("ACTIVIDAD", "PENDIENTE", 10), new ConteoEstado("ACTIVIDAD", "ATENDIDA", 4))));

        http.perform(comoGestor(get("/gestor/indicadores/embudo")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.etapas[1].etapa").value("ATENDIDAS"))
                .andExpect(jsonPath("$.etapas[1].porcentaje").value(40.0))
                .andExpect(jsonPath("$.porTipo.length()").value(4));
    }

    @Test
    @DisplayName("las categorías traen las pagadas")
    void categorias() throws Exception {
        when(distribucion.consultarDistribucion(any(), any())).thenReturn(DistribucionCategoria.de(OCTUBRE,
                List.of(new ConteoCategoria("MEDIANO", 5, 3))));

        http.perform(comoGestor(get("/gestor/indicadores/categorias")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.categorias[2].categoria").value("MEDIANO"))
                .andExpect(jsonPath("$.categorias[2].pagadas").value(3));
    }

    @Test
    @DisplayName("un periodo al revés es 400 DATO_INVALIDO, y una fecha mal escrita también")
    void periodoInvalido() throws Exception {
        when(tablero.consultarTablero(HASTA, DESDE)).thenThrow(new DatoInvalidoException("desde",
                "no puede ser posterior a hasta"));

        http.perform(comoGestor(get("/gestor/indicadores/tablero").param("desde", "2026-10-31")
                        .param("hasta", "2026-10-01")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("DATO_INVALIDO"));
        http.perform(comoGestor(get("/gestor/indicadores/tablero").param("desde", "1/10/2026")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("DATO_INVALIDO"));
    }

    @Test
    @DisplayName("solo el gestor del destino: el administrador no ve los indicadores")
    void soloElGestor() throws Exception {
        http.perform(get("/gestor/indicadores/tablero")
                        .header(HttpHeaders.AUTHORIZATION, TokensDePrueba.bearer(UUID.randomUUID(), "ADMINISTRADOR")))
                .andExpect(status().isForbidden());
        verifyNoInteractions(tablero);
    }

    private static MockHttpServletRequestBuilder comoGestor(MockHttpServletRequestBuilder peticion) {
        return peticion.header(HttpHeaders.AUTHORIZATION, TokensDePrueba.bearer(GESTOR, "GESTOR_DESTINO"));
    }
}
