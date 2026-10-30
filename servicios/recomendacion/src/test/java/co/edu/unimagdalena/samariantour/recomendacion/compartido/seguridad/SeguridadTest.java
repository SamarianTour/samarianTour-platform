package co.edu.unimagdalena.samariantour.recomendacion.compartido.seguridad;

import co.edu.unimagdalena.samariantour.seguridadjwt.TokensDePrueba;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Las rutas son públicas, pero un token que no sirve no pasa y las rutas con {@code ;} se rechazan (DR-22). */
@WebMvcTest(SeguridadTest.RutaDePrueba.class)
@Import(SeguridadTest.RutaDePrueba.class)
class SeguridadTest {

    private static final String USUARIO = "70000000-0000-0000-0000-000000000007";

    @Autowired
    private MockMvc http;

    @Test
    @DisplayName("sin token o con uno válido responde; con uno que no sirve, 401 NO_AUTENTICADO")
    void token() throws Exception {
        http.perform(get("/prueba")).andExpect(status().isOk());
        http.perform(get("/prueba").header(HttpHeaders.AUTHORIZATION, TokensDePrueba.bearer(USUARIO, "TURISTA")))
                .andExpect(status().isOk());
        http.perform(get("/prueba")
                        .header(HttpHeaders.AUTHORIZATION, TokensDePrueba.bearerConOtraClave(USUARIO, "TURISTA")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.codigo").value("NO_AUTENTICADO"));
    }

    @Test
    @DisplayName("una ruta con ; se rechaza con 400, como las que intentan esquivar a Nginx")
    void puntoYComa() throws Exception {
        http.perform(get("/actuator;x/health")).andExpect(status().isBadRequest());
    }

    @RestController
    static class RutaDePrueba {

        @GetMapping("/prueba")
        String prueba() {
            return "ok";
        }
    }
}
