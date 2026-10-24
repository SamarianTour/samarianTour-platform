package co.edu.unimagdalena.samariantour.recomendacion.recomendacion.infraestructura.entrada.rest;

import co.edu.unimagdalena.samariantour.recomendacion.recomendacion.dominio.excepcion.RecomendacionNoDisponibleException;
import co.edu.unimagdalena.samariantour.recomendacion.recomendacion.dominio.modelo.CriterioAfinidad;
import co.edu.unimagdalena.samariantour.recomendacion.recomendacion.dominio.modelo.Explicacion;
import co.edu.unimagdalena.samariantour.recomendacion.recomendacion.dominio.modelo.Idioma;
import co.edu.unimagdalena.samariantour.recomendacion.recomendacion.dominio.modelo.MotorRecomendacion;
import co.edu.unimagdalena.samariantour.recomendacion.recomendacion.dominio.modelo.NivelEsfuerzo;
import co.edu.unimagdalena.samariantour.recomendacion.recomendacion.dominio.modelo.Preferencias;
import co.edu.unimagdalena.samariantour.recomendacion.recomendacion.dominio.modelo.Recomendacion;
import co.edu.unimagdalena.samariantour.recomendacion.recomendacion.dominio.modelo.ResultadoRecomendacion;
import co.edu.unimagdalena.samariantour.recomendacion.recomendacion.dominio.modelo.TipoOferta;
import co.edu.unimagdalena.samariantour.recomendacion.recomendacion.dominio.puerto.entrada.RecomendarOfertasUseCase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static co.edu.unimagdalena.samariantour.recomendacion.recomendacion.OfertasDePrueba.oferta;
import static co.edu.unimagdalena.samariantour.recomendacion.recomendacion.dominio.modelo.AvisoRecomendacion.POCAS_COINCIDENCIAS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(RecomendacionController.class)
class RecomendacionControllerTest {

    @Autowired
    private MockMvc http;

    @MockitoBean
    private RecomendarOfertasUseCase recomendarOfertas;

    @Test
    @DisplayName("200 con el motor, la posición, la oferta para la tarjeta, los criterios y la explicación; sin prestador")
    void respuesta() throws Exception {
        when(recomendarOfertas.recomendar(any())).thenReturn(new ResultadoRecomendacion(MotorRecomendacion.IA, null,
                List.of(new Recomendacion(1, oferta("b0000000000000000000000a").nombre("Avistamiento de aves").crear(),
                        new Explicacion(List.of(CriterioAfinidad.ZONA), "Queda en Minca.")))));

        http.perform(post("/recomendaciones").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"texto\": \"aves\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.motor").value("IA"))
                .andExpect(jsonPath("$.aviso").doesNotExist())
                .andExpect(jsonPath("$.recomendaciones[0].posicion").value(1))
                .andExpect(jsonPath("$.recomendaciones[0].oferta.id").value("b0000000000000000000000a"))
                .andExpect(jsonPath("$.recomendaciones[0].oferta.nombre").value("Avistamiento de aves"))
                .andExpect(jsonPath("$.recomendaciones[0].oferta.tipo").value("ACTIVIDAD"))
                .andExpect(jsonPath("$.recomendaciones[0].oferta.zonaNombre").value("Minca"))
                .andExpect(jsonPath("$.recomendaciones[0].oferta.precioReferencia").value(80000))
                .andExpect(jsonPath("$.recomendaciones[0].oferta.unidadPrecio").value("por persona"))
                .andExpect(jsonPath("$.recomendaciones[0].oferta.prestadorId").doesNotExist())
                .andExpect(jsonPath("$.recomendaciones[0].criterios[0]").value("ZONA"))
                .andExpect(jsonPath("$.recomendaciones[0].explicacion").value("Queda en Minca."));
    }

    @Test
    @DisplayName("convierte la consulta en preferencias, con el idioma y los filtros")
    void consulta() throws Exception {
        when(recomendarOfertas.recomendar(any()))
                .thenReturn(new ResultadoRecomendacion(MotorRecomendacion.REGLAS, POCAS_COINCIDENCIAS, List.of()));

        http.perform(post("/recomendaciones").contentType(MediaType.APPLICATION_JSON).content("""
                        {"idioma": "en", "tiposOferta": ["ACTIVIDAD", "PAQUETE"], "accesibleSillaRuedas": true,
                         "nivelEsfuerzoMaximo": "MODERADO", "precioMaximo": 150000}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.aviso").value("POCAS_COINCIDENCIAS"))
                .andExpect(jsonPath("$.recomendaciones").isEmpty());

        ArgumentCaptor<Preferencias> preferencias = ArgumentCaptor.forClass(Preferencias.class);
        verify(recomendarOfertas).recomendar(preferencias.capture());
        assertThat(preferencias.getValue().idioma()).isEqualTo(Idioma.EN);
        assertThat(preferencias.getValue().tiposOferta()).containsExactly(TipoOferta.ACTIVIDAD, TipoOferta.PAQUETE);
        assertThat(preferencias.getValue().accesibleSillaRuedas()).isTrue();
        assertThat(preferencias.getValue().nivelEsfuerzoMaximo()).isEqualTo(NivelEsfuerzo.MODERADO);
        assertThat(preferencias.getValue().precioMaximo()).isEqualByComparingTo("150000");
    }

    @Test
    @DisplayName("sin texto ni filtros: 400 DATO_INVALIDO, sin llamar al caso de uso")
    void sinNada() throws Exception {
        http.perform(post("/recomendaciones").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("DATO_INVALIDO"));
        verifyNoInteractions(recomendarOfertas);
    }

    @Test
    @DisplayName("un tipo de oferta que no existe, un idioma no soportado o un JSON roto: 400 DATO_INVALIDO")
    void consultaInvalida() throws Exception {
        for (String cuerpo : List.of("{\"tiposOferta\": [\"CRUCERO\"]}", "{\"texto\": \"aves\", \"idioma\": \"fr\"}",
                "{\"texto\": ")) {
            http.perform(post("/recomendaciones").contentType(MediaType.APPLICATION_JSON).content(cuerpo))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.codigo").value("DATO_INVALIDO"));
        }
    }

    @Test
    @DisplayName("el catálogo no responde y no hay caché: 503 RECOMENDACION_NO_DISPONIBLE")
    void noDisponible() throws Exception {
        when(recomendarOfertas.recomendar(any()))
                .thenThrow(new RecomendacionNoDisponibleException("El catálogo no responde"));

        http.perform(post("/recomendaciones").contentType(MediaType.APPLICATION_JSON).content("{\"texto\": \"aves\"}"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.codigo").value("RECOMENDACION_NO_DISPONIBLE"));
    }
}
