package co.edu.unimagdalena.samariantour.recomendacion.recomendacion.infraestructura.entrada.rest;

import co.edu.unimagdalena.samariantour.recomendacion.recomendacion.dominio.excepcion.OfertaNoEncontradaException;
import co.edu.unimagdalena.samariantour.recomendacion.recomendacion.dominio.excepcion.RecomendacionNoDisponibleException;
import co.edu.unimagdalena.samariantour.recomendacion.recomendacion.dominio.modelo.CriterioAfinidad;
import co.edu.unimagdalena.samariantour.recomendacion.recomendacion.dominio.modelo.Explicacion;
import co.edu.unimagdalena.samariantour.recomendacion.recomendacion.dominio.modelo.Idioma;
import co.edu.unimagdalena.samariantour.recomendacion.recomendacion.dominio.modelo.Recomendacion;
import co.edu.unimagdalena.samariantour.recomendacion.recomendacion.dominio.modelo.ResultadoAlternativas;
import co.edu.unimagdalena.samariantour.recomendacion.recomendacion.dominio.puerto.entrada.ProponerAlternativasUseCase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static co.edu.unimagdalena.samariantour.recomendacion.recomendacion.OfertasDePrueba.oferta;
import static co.edu.unimagdalena.samariantour.recomendacion.recomendacion.dominio.modelo.AvisoRecomendacion.POCAS_COINCIDENCIAS;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AlternativaController.class)
class AlternativaControllerTest {

    private static final String TAYRONA = "a00000000000000000000005";

    @Autowired
    private MockMvc http;

    @MockitoBean
    private ProponerAlternativasUseCase proponerAlternativas;

    @Test
    @DisplayName("aplica: la zona de origen y cada alternativa con la forma de una recomendación, sin prestador")
    void aplica() throws Exception {
        when(proponerAlternativas.proponer("b1", Idioma.EN)).thenReturn(new ResultadoAlternativas(true,
                POCAS_COINCIDENCIAS, new ResultadoAlternativas.ZonaOrigen(TAYRONA, "Parque Tayrona"), List.of(
                new Recomendacion(1, oferta("b7").nombre("Caminata a Pozo Azul").crear(), new Explicacion(
                        List.of(CriterioAfinidad.TIPO_OFERTA, CriterioAfinidad.TIPO_ACTIVIDAD),
                        "Same type of offer in Minca, a less crowded area; also hiking.")))));

        http.perform(get("/ofertas/{ofertaId}/alternativas", "b1").param("idioma", "en"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.aplica").value(true))
                .andExpect(jsonPath("$.aviso").value("POCAS_COINCIDENCIAS"))
                .andExpect(jsonPath("$.zonaOrigen.id").value(TAYRONA))
                .andExpect(jsonPath("$.zonaOrigen.nombre").value("Parque Tayrona"))
                .andExpect(jsonPath("$.alternativas[0].posicion").value(1))
                .andExpect(jsonPath("$.alternativas[0].oferta.id").value("b7"))
                .andExpect(jsonPath("$.alternativas[0].oferta.zonaNombre").value("Minca"))
                .andExpect(jsonPath("$.alternativas[0].oferta.prestadorId").doesNotExist())
                .andExpect(jsonPath("$.alternativas[0].criterios[1]").value("TIPO_ACTIVIDAD"))
                .andExpect(jsonPath("$.alternativas[0].explicacion").value(
                        "Same type of offer in Minca, a less crowded area; also hiking."));
    }

    @Test
    @DisplayName("no aplica: 200 con aplica false y la lista vacía, sin aviso ni zona; sin idioma, español")
    void noAplica() throws Exception {
        when(proponerAlternativas.proponer("b6", Idioma.ES)).thenReturn(ResultadoAlternativas.noAplica());

        http.perform(get("/ofertas/{ofertaId}/alternativas", "b6"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.aplica").value(false))
                .andExpect(jsonPath("$.alternativas").isEmpty())
                .andExpect(jsonPath("$.aviso").doesNotExist())
                .andExpect(jsonPath("$.zonaOrigen").doesNotExist());
    }

    @Test
    @DisplayName("404 OFERTA_NO_ENCONTRADA y 503 RECOMENDACION_NO_DISPONIBLE")
    void errores() throws Exception {
        when(proponerAlternativas.proponer("no-existe", Idioma.ES))
                .thenThrow(new OfertaNoEncontradaException("no-existe"));
        when(proponerAlternativas.proponer("b1", Idioma.ES))
                .thenThrow(new RecomendacionNoDisponibleException("El catálogo no responde"));

        http.perform(get("/ofertas/{ofertaId}/alternativas", "no-existe"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.codigo").value("OFERTA_NO_ENCONTRADA"));
        http.perform(get("/ofertas/{ofertaId}/alternativas", "b1"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.codigo").value("RECOMENDACION_NO_DISPONIBLE"));
    }

    @Test
    @DisplayName("un idioma que no es es ni en: 400, sin llamar al caso de uso")
    void idiomaInvalido() throws Exception {
        http.perform(get("/ofertas/{ofertaId}/alternativas", "b1").param("idioma", "fr"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("DATO_INVALIDO"));
        verifyNoInteractions(proponerAlternativas);
    }
}
