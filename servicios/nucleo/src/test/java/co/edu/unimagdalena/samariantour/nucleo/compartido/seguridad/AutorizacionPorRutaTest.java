package co.edu.unimagdalena.samariantour.nucleo.compartido.seguridad;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * La autorización por prefijo de ruta (DN-05) con un controlador de prueba en cada prefijo: {@code /prestador/**}
 * exige PRESTADOR, {@code /admin/**} ADMINISTRADOR, {@code /turista/**} TURISTA, {@code /operador/**}
 * OPERADOR_DISPONIBILIDAD y {@code /gestor/**} GESTOR_DESTINO (DN-04); las demás rutas son públicas.
 */
@WebMvcTest(AutorizacionPorRutaTest.RutasDePrueba.class)
@Import({IdentidadPorHeaders.class, AutorizacionPorRutaTest.RutasDePrueba.class})
@ActiveProfiles("demo")
class AutorizacionPorRutaTest {

    private static final String USUARIO = "30000000-0000-0000-0000-000000000003";

    @Autowired
    private MockMvc http;

    @Test
    @DisplayName("cada prefijo deja pasar su rol y el controlador recibe la identidad verificada")
    void cadaRolEnSuPrefijo() throws Exception {
        http.perform(como(get("/prestador/prueba"), "PRESTADOR").header("X-Tipo-Prestador", "ALOJAMIENTO"))
                .andExpect(status().isOk())
                .andExpect(content().string("PRESTADOR " + USUARIO + " ALOJAMIENTO"));
        http.perform(como(get("/admin/prueba"), "ADMINISTRADOR"))
                .andExpect(status().isOk())
                .andExpect(content().string("ADMINISTRADOR " + USUARIO + " null"));
        http.perform(como(get("/turista/prueba"), "TURISTA"))
                .andExpect(status().isOk())
                .andExpect(content().string("TURISTA " + USUARIO + " null"));
        http.perform(como(get("/operador/prueba"), "OPERADOR_DISPONIBILIDAD"))
                .andExpect(status().isOk())
                .andExpect(content().string("OPERADOR_DISPONIBILIDAD " + USUARIO + " null"));
        http.perform(como(get("/gestor/prueba"), "GESTOR_DESTINO"))
                .andExpect(status().isOk())
                .andExpect(content().string("GESTOR_DESTINO " + USUARIO + " null"));
    }

    @Test
    @DisplayName("un rol en el prefijo de otro responde 403 ACCESO_DENEGADO")
    void rolEquivocado() throws Exception {
        http.perform(como(get("/admin/prueba"), "TURISTA"))
                .andExpect(status().isForbidden())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.codigo").value("ACCESO_DENEGADO"));
        http.perform(como(get("/turista/prueba"), "ADMINISTRADOR"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.codigo").value("ACCESO_DENEGADO"));
        http.perform(como(get("/prestador/prueba"), "TURISTA"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.codigo").value("ACCESO_DENEGADO"));
        http.perform(como(get("/operador/prueba"), "GESTOR_DESTINO"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.codigo").value("ACCESO_DENEGADO"));
        http.perform(como(get("/gestor/prueba"), "OPERADOR_DISPONIBILIDAD"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.codigo").value("ACCESO_DENEGADO"));
    }

    @Test
    @DisplayName("sin identidad, las rutas protegidas responden 401 NO_AUTENTICADO y las públicas no la piden")
    void sinIdentidad() throws Exception {
        for (String ruta : new String[] {"/prestador/prueba", "/admin/prueba", "/turista/prueba", "/operador/prueba",
                "/gestor/prueba"}) {
            http.perform(get(ruta))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.codigo").value("NO_AUTENTICADO"));
        }
        http.perform(get("/publica/prueba")).andExpect(status().isOk());
    }

    private static MockHttpServletRequestBuilder como(MockHttpServletRequestBuilder peticion, String rol) {
        return peticion.header("X-Usuario-Id", USUARIO).header("X-Rol", rol);
    }

    /** Una ruta por prefijo; responde la identidad que recibió. */
    @RestController
    static class RutasDePrueba {

        @GetMapping({"/prestador/prueba", "/admin/prueba", "/turista/prueba", "/operador/prueba", "/gestor/prueba"})
        String protegida(HttpServletRequest peticion) {
            Identidad identidad = Identidad.de(peticion);
            return identidad.rol() + " " + identidad.usuarioId() + " " + identidad.tipoPrestador();
        }

        @GetMapping("/publica/prueba")
        String publica() {
            return "ok";
        }
    }
}
