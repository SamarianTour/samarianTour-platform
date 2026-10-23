package co.edu.unimagdalena.samariantour.catalogo;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * La salud que consulta el healthcheck de Docker Compose. Con los datos en memoria no depende de MongoDB.
 */
@SpringBootTest(properties = "spring.autoconfigure.exclude="
        + "org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration,"
        + "org.springframework.boot.autoconfigure.data.mongo.MongoDataAutoConfiguration")
@AutoConfigureMockMvc
@ActiveProfiles({"memoria", "demo"})
class SaludApiTest {

    @Autowired
    private MockMvc http;

    @Test
    @DisplayName("el servicio responde su salud sin necesitar MongoDB")
    void salud() throws Exception {
        http.perform(get("/actuator/health")).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    @DisplayName("de Actuator solo se exponen la salud y las métricas")
    void soloLaSaludYLasMetricas() throws Exception {
        http.perform(get("/actuator/env")).andExpect(status().isNotFound());
        http.perform(get("/actuator/beans")).andExpect(status().isNotFound());
    }
}
