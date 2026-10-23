package co.edu.unimagdalena.samariantour.catalogo;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.actuate.observability.AutoConfigureObservability;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Las métricas con que se mide el p95 de la búsqueda en el servidor (QAS-07, DC-48). Las pruebas de Spring Boot
 * apagan las métricas por defecto; {@link AutoConfigureObservability} las enciende como en el servicio.
 */
@SpringBootTest(properties = "spring.autoconfigure.exclude="
        + "org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration,"
        + "org.springframework.boot.autoconfigure.data.mongo.MongoDataAutoConfiguration")
@AutoConfigureMockMvc
@AutoConfigureObservability(tracing = false)
@ActiveProfiles({"memoria", "demo"})
class MetricasApiTest {

    @Autowired
    private MockMvc http;

    @Test
    @DisplayName("una búsqueda queda medida por ruta, con el umbral de 2 s de QAS-07")
    void busquedaMedida() throws Exception {
        http.perform(get("/ofertas").param("tipo", "ACTIVIDAD").param("zonaId", "a00000000000000000000001")
                        .param("precioMax", "200000"))
                .andExpect(status().isOk());

        String metricas = http.perform(get("/actuator/prometheus"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertThat(metricas.lines().filter(linea -> linea.startsWith("http_server_requests_seconds"))
                .filter(linea -> linea.contains("uri=\"/ofertas\"")))
                .anySatisfy(linea -> assertThat(linea).contains("le=\"2.0\""))
                .anySatisfy(linea -> assertThat(linea).startsWith("http_server_requests_seconds_count"));
    }
}
