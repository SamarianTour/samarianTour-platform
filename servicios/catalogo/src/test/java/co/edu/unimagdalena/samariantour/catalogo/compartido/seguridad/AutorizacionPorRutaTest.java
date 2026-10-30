package co.edu.unimagdalena.samariantour.catalogo.compartido.seguridad;

import co.edu.unimagdalena.samariantour.seguridadjwt.TokensDePrueba;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.stream.Stream;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * La batería de acceso cruzado de QAS-04 sobre las reglas reales ({@code SeguridadConfig}, DC-01, DC-13), con un
 * controlador de prueba en cada prefijo: cada rol entra solo al suyo, los roles que el catálogo no distingue no entran
 * a ninguno, y ningún token falso, vencido o de otro emisor pasa.
 */
@WebMvcTest(AutorizacionPorRutaTest.RutasDePrueba.class)
@Import(AutorizacionPorRutaTest.RutasDePrueba.class)
class AutorizacionPorRutaTest {

    private static final String USUARIO = "30000000-0000-0000-0000-000000000003";

    /** El rol que exige cada prefijo. */
    private static final Map<String, String> PREFIJOS = Map.of(
            "/prestador/prueba", "PRESTADOR",
            "/admin/prueba", "ADMINISTRADOR",
            "/turista/prueba", "TURISTA");

    /** Los cinco roles que puede traer el token del núcleo. */
    private static final String[] ROLES_DEL_TOKEN = {"TURISTA", "PRESTADOR", "ADMINISTRADOR",
            "OPERADOR_DISPONIBILIDAD", "GESTOR_DESTINO"};

    @Autowired
    private MockMvc http;

    static Stream<Arguments> rutaYRol() {
        return PREFIJOS.keySet().stream()
                .flatMap(ruta -> Stream.of(ROLES_DEL_TOKEN).map(rol -> Arguments.of(ruta, rol)));
    }

    @ParameterizedTest(name = "{1} en {0}")
    @MethodSource("rutaYRol")
    @DisplayName("cada rol entra solo a su prefijo; en los demás, 403 ACCESO_DENEGADO")
    void accesoCruzado(String ruta, String rol) throws Exception {
        if (PREFIJOS.get(ruta).equals(rol)) {
            http.perform(get(ruta).header(HttpHeaders.AUTHORIZATION, TokensDePrueba.bearer(USUARIO, rol)))
                    .andExpect(status().isOk())
                    .andExpect(content().string(rol + " " + USUARIO + " null"));
        } else {
            http.perform(get(ruta).header(HttpHeaders.AUTHORIZATION, TokensDePrueba.bearer(USUARIO, rol)))
                    .andExpect(status().isForbidden())
                    .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                    .andExpect(jsonPath("$.codigo").value("ACCESO_DENEGADO"));
        }
    }

    @Test
    @DisplayName("el controlador recibe el tipo del prestador del token")
    void tipoDelPrestador() throws Exception {
        http.perform(get("/prestador/prueba")
                        .header(HttpHeaders.AUTHORIZATION, TokensDePrueba.bearerPrestador(USUARIO, "ALOJAMIENTO")))
                .andExpect(content().string("PRESTADOR " + USUARIO + " ALOJAMIENTO"));
    }

    @Test
    @DisplayName("sin token, 401 NO_AUTENTICADO; las rutas públicas no lo piden y aceptan el de cualquier rol")
    void sinToken() throws Exception {
        for (String ruta : PREFIJOS.keySet()) {
            http.perform(get(ruta))
                    .andExpect(status().isUnauthorized())
                    .andExpect(header().string(HttpHeaders.WWW_AUTHENTICATE, "Bearer"))
                    .andExpect(jsonPath("$.codigo").value("NO_AUTENTICADO"));
        }
        http.perform(get("/publica/prueba")).andExpect(status().isOk());
        http.perform(get("/publica/prueba")
                        .header(HttpHeaders.AUTHORIZATION, TokensDePrueba.bearer(USUARIO, "OPERADOR_DISPONIBILIDAD")))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("un token vencido, de otra clave, de otro emisor o mal formado responde 401, aun con el rol correcto")
    void tokensQueNoSirven() throws Exception {
        for (String token : new String[] {
                TokensDePrueba.bearerVencido(USUARIO, "ADMINISTRADOR"),
                TokensDePrueba.bearerConOtraClave(USUARIO, "ADMINISTRADOR"),
                TokensDePrueba.bearerAjustado(USUARIO, "ADMINISTRADOR", datos -> datos.issuer("otro")),
                "Bearer no-es-un-jwt"}) {
            http.perform(get("/admin/prueba").header(HttpHeaders.AUTHORIZATION, token))
                    .andExpect(status().isUnauthorized())
                    .andExpect(header().string(HttpHeaders.WWW_AUTHENTICATE, "Bearer error=\"invalid_token\""))
                    .andExpect(jsonPath("$.codigo").value("NO_AUTENTICADO"));
        }
    }

    @Test
    @DisplayName("los headers de la demo ya no identifican a nadie")
    void headersDeLaDemo() throws Exception {
        http.perform(get("/admin/prueba").header("X-Usuario-Id", USUARIO).header("X-Rol", "ADMINISTRADOR"))
                .andExpect(status().isUnauthorized());
    }

    @ParameterizedTest
    @ValueSource(strings = {"/admin;x/prueba", "/internal;x/prueba", "/publica/../admin/prueba"})
    @DisplayName("una ruta con ; o .. se rechaza antes de llegar a las reglas, para que no las esquive")
    void rutasQueEsquivanLasReglas(String ruta) throws Exception {
        http.perform(get(ruta).header(HttpHeaders.AUTHORIZATION, TokensDePrueba.bearer(USUARIO, "TURISTA")))
                .andExpect(status().isBadRequest());
    }

    /** Una ruta por prefijo; responde la identidad que recibió. */
    @RestController
    static class RutasDePrueba {

        @GetMapping({"/prestador/prueba", "/admin/prueba", "/turista/prueba"})
        String protegida(HttpServletRequest peticion) {
            Identidad identidad = Identidad.de(peticion);
            return identidad.rol() + " " + identidad.usuarioId() + " " + identidad.tipoPrestador();
        }

        @GetMapping({"/publica/prueba", "/internal/prueba"})
        String publica() {
            return "ok";
        }
    }
}
