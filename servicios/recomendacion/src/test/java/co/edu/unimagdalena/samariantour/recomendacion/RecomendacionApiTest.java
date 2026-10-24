package co.edu.unimagdalena.samariantour.recomendacion;

import co.edu.unimagdalena.samariantour.recomendacion.recomendacion.dominio.modelo.OfertaCandidata;
import co.edu.unimagdalena.samariantour.recomendacion.recomendacion.dominio.puerto.salida.CatalogoConsultaPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;

import static co.edu.unimagdalena.samariantour.recomendacion.recomendacion.OfertasDePrueba.oferta;
import static co.edu.unimagdalena.samariantour.recomendacion.recomendacion.dominio.modelo.TipoActividad.AVISTAMIENTO_FAUNA;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * El servicio completo con la configuración por defecto (IA apagada) y un catálogo de prueba: nunca llama a un
 * proveedor de IA ni al catálogo real.
 */
@SpringBootTest(properties = "recomendacion.ia.api-key=clave-que-no-debe-aparecer")
@AutoConfigureMockMvc
@ExtendWith(OutputCaptureExtension.class)
class RecomendacionApiTest {

    @Autowired
    private MockMvc http;

    @MockitoBean
    private CatalogoConsultaPort catalogo;

    @BeforeEach
    void catalogoDePrueba() {
        List<OfertaCandidata> ofertas = IntStream.rangeClosed(1, 8)
                .mapToObj(i -> oferta("b" + i).actividades(AVISTAMIENTO_FAUNA)
                        .prestador(UUID.fromString("00000000-0000-0000-0000-00000000000" + i)).crear())
                .toList();
        when(catalogo.ofertasVisibles()).thenReturn(ofertas);
    }

    @Test
    @DisplayName("con la IA apagada responde el motor por reglas: 5 recomendaciones explicadas por plantilla")
    void porReglas() throws Exception {
        http.perform(post("/recomendaciones").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tiposActividad\": [\"AVISTAMIENTO_FAUNA\"]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.motor").value("REGLAS"))
                .andExpect(jsonPath("$.recomendaciones.length()").value(5))
                .andExpect(jsonPath("$.recomendaciones[0].criterios[0]").value("TIPO_ACTIVIDAD"))
                .andExpect(jsonPath("$.recomendaciones[0].explicacion").value("Coincide con: avistamiento de fauna."));
    }

    @Test
    @DisplayName("el log de exposición lleva motor, ofertas y prestadores, pero ni el texto del turista ni la key")
    void exposicionSinDatosDelTurista(CapturedOutput salida) throws Exception {
        http.perform(post("/recomendaciones").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"texto\": \"Soy Mariana, viajo con mi hija y quiero ver aves\"}"))
                .andExpect(status().isOk());

        assertThat(salida.getOut()).contains("exposicion", "\"motor\":\"REGLAS\"", "\"prestadorId\":\"00000000-")
                .doesNotContain("Mariana", "clave-que-no-debe-aparecer");
    }
}
