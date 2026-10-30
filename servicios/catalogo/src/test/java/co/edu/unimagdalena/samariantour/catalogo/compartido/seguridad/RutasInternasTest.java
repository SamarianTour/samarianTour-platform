package co.edu.unimagdalena.samariantour.catalogo.compartido.seguridad;

import co.edu.unimagdalena.samariantour.seguridadjwt.ServiciosInternos;
import co.edu.unimagdalena.samariantour.seguridadjwt.TokensDePrueba;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Las reglas de {@code /internal/**} ({@code SeguridadConfig}, DN-38) con un controlador de prueba en las mismas rutas
 * que las reales: cada ruta admite solo al servicio que la usa, y ningún token de usuario, falso o para otro servicio
 * entra.
 */
@WebMvcTest(RutasInternasTest.RutasDePrueba.class)
@Import(RutasInternasTest.RutasDePrueba.class)
class RutasInternasTest {

    private static final String NUCLEO = ServiciosInternos.NUCLEO;
    private static final String RECOMENDACION = ServiciosInternos.RECOMENDACION;

    @Autowired
    private MockMvc http;

    @ParameterizedTest(name = "{0} {1}: {2} {3}")
    @CsvSource({
            "POST, /internal/avisos, nucleo, 200",
            "POST, /internal/avisos, recomendacion, 403",
            "POST, /internal/opiniones/avisos, nucleo, 200",
            "POST, /internal/opiniones/avisos, recomendacion, 403",
            "GET, /internal/ofertas/b1, nucleo, 200",
            "GET, /internal/ofertas/b1, recomendacion, 403",
            "GET, /internal/moderacion/ofertas/b1, nucleo, 200",
            "GET, /internal/moderacion/opiniones/d1, recomendacion, 403",
            "GET, /internal/ofertas, recomendacion, 200",
            "GET, /internal/ofertas, nucleo, 403",
            "GET, /internal/otra, nucleo, 403"})
    @DisplayName("cada ruta interna admite solo al servicio que la usa; en las demás, 403 ACCESO_DENEGADO")
    void porServicio(String metodo, String ruta, String servicio, int esperado) throws Exception {
        http.perform(peticion(metodo, ruta)
                        .header(HttpHeaders.AUTHORIZATION, TokensDePrueba.bearerServicio(servicio,
                                ServiciosInternos.CATALOGO)))
                .andExpect(status().is(esperado));
    }

    @Test
    @DisplayName("sin token, con el de un usuario (aun el del administrador), con otra clave o para otro servicio: 401")
    void tokensQueNoSirven() throws Exception {
        for (String token : new String[] {
                TokensDePrueba.bearer("60000000-0000-0000-0000-000000000006", "ADMINISTRADOR"),
                TokensDePrueba.bearerServicioConOtraClave(NUCLEO, ServiciosInternos.CATALOGO),
                TokensDePrueba.bearerServicio(NUCLEO, RECOMENDACION)}) {
            http.perform(post("/internal/avisos").header(HttpHeaders.AUTHORIZATION, token))
                    .andExpect(status().isUnauthorized())
                    .andExpect(header().string(HttpHeaders.WWW_AUTHENTICATE, "Bearer error=\"invalid_token\""))
                    .andExpect(jsonPath("$.codigo").value("NO_AUTENTICADO"));
        }
        http.perform(post("/internal/avisos"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.codigo").value("NO_AUTENTICADO"));
    }

    @Test
    @DisplayName("el token de servicio no sirve en las rutas de los usuarios")
    void servicioEnRutaDeUsuario() throws Exception {
        http.perform(get("/admin/prueba")
                        .header(HttpHeaders.AUTHORIZATION, TokensDePrueba.bearerServicio(NUCLEO,
                                ServiciosInternos.CATALOGO)))
                .andExpect(status().isUnauthorized());
    }

    private static MockHttpServletRequestBuilder peticion(String metodo, String ruta) {
        return "POST".equals(metodo) ? post(ruta) : get(ruta);
    }

    /** Las rutas internas de verdad, más una de usuario; responden 200 si la petición llega. */
    @RestController
    static class RutasDePrueba {

        @PostMapping({"/internal/avisos", "/internal/opiniones/avisos"})
        String aviso() {
            return "ok";
        }

        @GetMapping({"/internal/ofertas", "/internal/ofertas/{id}", "/internal/moderacion/ofertas/{id}",
                "/internal/moderacion/opiniones/{id}", "/internal/otra", "/admin/prueba"})
        String consulta() {
            return "ok";
        }
    }
}
