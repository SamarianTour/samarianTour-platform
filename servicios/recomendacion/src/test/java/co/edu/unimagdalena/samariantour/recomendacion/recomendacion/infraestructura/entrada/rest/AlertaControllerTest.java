package co.edu.unimagdalena.samariantour.recomendacion.recomendacion.infraestructura.entrada.rest;

import co.edu.unimagdalena.samariantour.recomendacion.recomendacion.dominio.excepcion.AlertasNoDisponiblesException;
import co.edu.unimagdalena.samariantour.recomendacion.recomendacion.dominio.modelo.AlertaClimatica;
import co.edu.unimagdalena.samariantour.recomendacion.recomendacion.dominio.modelo.AlertasDeZona;
import co.edu.unimagdalena.samariantour.recomendacion.recomendacion.dominio.modelo.NivelAlerta;
import co.edu.unimagdalena.samariantour.recomendacion.recomendacion.dominio.modelo.ResumenAlertas;
import co.edu.unimagdalena.samariantour.recomendacion.recomendacion.dominio.modelo.TextoTraducido;
import co.edu.unimagdalena.samariantour.recomendacion.recomendacion.dominio.modelo.TipoAlerta;
import co.edu.unimagdalena.samariantour.recomendacion.recomendacion.dominio.puerto.entrada.ConsultarAlertasZonaUseCase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AlertaController.class)
class AlertaControllerTest {

    private static final String TAYRONA = "a00000000000000000000005";
    private static final Instant ACTUALIZADO = Instant.parse("2026-10-24T15:00:00Z");

    @Autowired
    private MockMvc http;

    @MockitoBean
    private ConsultarAlertasZonaUseCase consultarAlertas;

    private final AlertaClimatica cierre = new AlertaClimatica("simulada-tayrona-cierre", TAYRONA, TipoAlerta.CIERRE,
            NivelAlerta.NARANJA, new TextoTraducido("Cierre temporal del parque", "Temporary park closure"),
            new TextoTraducido("Recuperación ecológica.", "Ecological recovery."),
            "Parques Nacionales (simulado)", Instant.parse("2026-10-24T13:00:00Z"), null);

    @Test
    @DisplayName("GET /zonas/{zonaId}/alertas: la zona, la hora de la lectura y sus alertas; sin vigenteHasta si no tiene fin")
    void deZona() throws Exception {
        when(consultarAlertas.deZona(TAYRONA)).thenReturn(new AlertasDeZona(TAYRONA, ACTUALIZADO, List.of(cierre)));

        http.perform(get("/zonas/{zonaId}/alertas", TAYRONA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.zonaId").value(TAYRONA))
                .andExpect(jsonPath("$.actualizadoEn").value("2026-10-24T15:00:00Z"))
                .andExpect(jsonPath("$.alertas[0].id").value("simulada-tayrona-cierre"))
                .andExpect(jsonPath("$.alertas[0].tipo").value("CIERRE"))
                .andExpect(jsonPath("$.alertas[0].nivel").value("NARANJA"))
                .andExpect(jsonPath("$.alertas[0].titulo").value("Cierre temporal del parque"))
                .andExpect(jsonPath("$.alertas[0].descripcion").value("Recuperación ecológica."))
                .andExpect(jsonPath("$.alertas[0].fuente").value("Parques Nacionales (simulado)"))
                .andExpect(jsonPath("$.alertas[0].emitidaEn").value("2026-10-24T13:00:00Z"))
                .andExpect(jsonPath("$.alertas[0].vigenteHasta").doesNotExist());
    }

    @Test
    @DisplayName("?idioma=en: título y descripción en inglés en los dos; la fuente no se traduce")
    void enIngles() throws Exception {
        when(consultarAlertas.deZona(TAYRONA)).thenReturn(new AlertasDeZona(TAYRONA, ACTUALIZADO, List.of(cierre)));
        when(consultarAlertas.todas()).thenReturn(new ResumenAlertas(ACTUALIZADO, List.of(
                new ResumenAlertas.Zona(TAYRONA, "Parque Tayrona", List.of(cierre)))));

        http.perform(get("/zonas/{zonaId}/alertas", TAYRONA).param("idioma", "en"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.alertas[0].titulo").value("Temporary park closure"))
                .andExpect(jsonPath("$.alertas[0].descripcion").value("Ecological recovery."))
                .andExpect(jsonPath("$.alertas[0].fuente").value("Parques Nacionales (simulado)"));
        http.perform(get("/alertas").param("idioma", "EN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.zonas[0].zonaNombre").value("Parque Tayrona"))
                .andExpect(jsonPath("$.zonas[0].alertas[0].titulo").value("Temporary park closure"));
    }

    @Test
    @DisplayName("un idioma que no es es ni en: 400 DATO_INVALIDO, sin consultar las alertas")
    void idiomaInvalido() throws Exception {
        http.perform(get("/zonas/{zonaId}/alertas", TAYRONA).param("idioma", "fr"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("DATO_INVALIDO"));
        http.perform(get("/alertas").param("idioma", "fr"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(consultarAlertas);
    }

    @Test
    @DisplayName("sin descripción, el campo se omite")
    void sinDescripcion() throws Exception {
        AlertaClimatica sinTexto = new AlertaClimatica("a", TAYRONA, TipoAlerta.OTRA, NivelAlerta.AMARILLA,
                new TextoTraducido("Aviso", "Notice"), null, "IDEAM (simulado)", null, null);
        when(consultarAlertas.deZona(TAYRONA)).thenReturn(new AlertasDeZona(TAYRONA, ACTUALIZADO, List.of(sinTexto)));

        http.perform(get("/zonas/{zonaId}/alertas", TAYRONA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.alertas[0].descripcion").doesNotExist());
    }

    @Test
    @DisplayName("una zona sin alertas activas responde 200 con la lista vacía")
    void sinAlertas() throws Exception {
        when(consultarAlertas.deZona("a00000000000000000000001"))
                .thenReturn(new AlertasDeZona("a00000000000000000000001", ACTUALIZADO, List.of()));

        http.perform(get("/zonas/{zonaId}/alertas", "a00000000000000000000001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.alertas").isEmpty());
    }

    @Test
    @DisplayName("GET /alertas: agrupadas por zona; sin zonaNombre si el catálogo no lo dio")
    void todas() throws Exception {
        when(consultarAlertas.todas()).thenReturn(new ResumenAlertas(ACTUALIZADO, List.of(
                new ResumenAlertas.Zona(TAYRONA, "Parque Tayrona", List.of(cierre)),
                new ResumenAlertas.Zona("a00000000000000000000099", null, List.of(cierre)))));

        http.perform(get("/alertas"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.actualizadoEn").value("2026-10-24T15:00:00Z"))
                .andExpect(jsonPath("$.zonas[0].zonaNombre").value("Parque Tayrona"))
                .andExpect(jsonPath("$.zonas[0].alertas[0].titulo").value("Cierre temporal del parque"))
                .andExpect(jsonPath("$.zonas[1].zonaNombre").doesNotExist());
    }

    @Test
    @DisplayName("sin ninguna lectura de la fuente: 503 ALERTAS_NO_DISPONIBLES en los dos")
    void noDisponibles() throws Exception {
        when(consultarAlertas.deZona(TAYRONA)).thenThrow(new AlertasNoDisponiblesException());
        when(consultarAlertas.todas()).thenThrow(new AlertasNoDisponiblesException());

        http.perform(get("/zonas/{zonaId}/alertas", TAYRONA))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.codigo").value("ALERTAS_NO_DISPONIBLES"));
        http.perform(get("/alertas"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.codigo").value("ALERTAS_NO_DISPONIBLES"));
    }
}
