package co.edu.unimagdalena.samariantour.nucleo.compartido.seguridad;

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

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Stream;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * La batería de acceso cruzado de QAS-04 sobre las reglas reales ({@code SeguridadConfig}, DN-37), con un controlador
 * de prueba en cada prefijo: cada rol entra solo al suyo, {@code /cuenta/**} admite los cinco, y ningún token falso,
 * vencido o de otro emisor pasa.
 */
@WebMvcTest(AutorizacionPorRutaTest.RutasDePrueba.class)
@Import(AutorizacionPorRutaTest.RutasDePrueba.class)
class AutorizacionPorRutaTest {

    private static final String USUARIO = "30000000-0000-0000-0000-000000000003";

    /** El rol que exige cada prefijo (DN-04). */
    private static final Map<String, Rol> PREFIJOS = Map.of(
            "/prestador/prueba", Rol.PRESTADOR,
            "/admin/prueba", Rol.ADMINISTRADOR,
            "/turista/prueba", Rol.TURISTA,
            "/operador/prueba", Rol.OPERADOR_DISPONIBILIDAD,
            "/gestor/prueba", Rol.GESTOR_DESTINO);

    @Autowired
    private MockMvc http;

    static Stream<Arguments> rutaYRol() {
        return PREFIJOS.keySet().stream()
                .flatMap(ruta -> Arrays.stream(Rol.values()).map(rol -> Arguments.of(ruta, rol)));
    }

    @ParameterizedTest(name = "{1} en {0}")
    @MethodSource("rutaYRol")
    @DisplayName("cada rol entra solo a su prefijo; en los demás, 403 ACCESO_DENEGADO")
    void accesoCruzado(String ruta, Rol rol) throws Exception {
        if (PREFIJOS.get(ruta) == rol) {
            http.perform(get(ruta).header(HttpHeaders.AUTHORIZATION, TokensDePrueba.bearer(USUARIO, rol.name())))
                    .andExpect(status().isOk())
                    .andExpect(content().string(rol + " " + USUARIO + " null"));
        } else {
            http.perform(get(ruta).header(HttpHeaders.AUTHORIZATION, TokensDePrueba.bearer(USUARIO, rol.name())))
                    .andExpect(status().isForbidden())
                    .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                    .andExpect(jsonPath("$.codigo").value("ACCESO_DENEGADO"));
        }
    }

    @ParameterizedTest
    @MethodSource("roles")
    @DisplayName("/cuenta/** admite los cinco roles (RF-12)")
    void cuentaDeCualquierRol(Rol rol) throws Exception {
        http.perform(get("/cuenta/prueba")
                        .header(HttpHeaders.AUTHORIZATION, TokensDePrueba.bearer(USUARIO, rol.name())))
                .andExpect(status().isOk());
    }

    static Stream<Rol> roles() {
        return Arrays.stream(Rol.values());
    }

    @Test
    @DisplayName("el controlador recibe el tipo del prestador; sin RNT registrado, el prestador entra sin tipo")
    void tipoDelPrestador() throws Exception {
        http.perform(get("/prestador/prueba")
                        .header(HttpHeaders.AUTHORIZATION, TokensDePrueba.bearerPrestador(USUARIO, "ALOJAMIENTO")))
                .andExpect(content().string("PRESTADOR " + USUARIO + " ALOJAMIENTO"));
        http.perform(get("/prestador/prueba")
                        .header(HttpHeaders.AUTHORIZATION, TokensDePrueba.bearerPrestador(USUARIO, null)))
                .andExpect(content().string("PRESTADOR " + USUARIO + " null"));
    }

    @Test
    @DisplayName("sin token, 401 NO_AUTENTICADO con WWW-Authenticate; las rutas públicas no lo piden")
    void sinToken() throws Exception {
        for (String ruta : PREFIJOS.keySet()) {
            http.perform(get(ruta))
                    .andExpect(status().isUnauthorized())
                    .andExpect(header().string(HttpHeaders.WWW_AUTHENTICATE, "Bearer"))
                    .andExpect(jsonPath("$.codigo").value("NO_AUTENTICADO"));
        }
        http.perform(get("/cuenta/prueba")).andExpect(status().isUnauthorized());
        http.perform(get("/publica/prueba")).andExpect(status().isOk());
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
    @ValueSource(strings = {"/admin;x/prueba", "/admin/prueba;jsessionid=1", "/publica/../admin/prueba"})
    @DisplayName("una ruta con ; o .. se rechaza antes de llegar a las reglas, para que no las esquive")
    void rutasQueEsquivanLasReglas(String ruta) throws Exception {
        http.perform(get(ruta).header(HttpHeaders.AUTHORIZATION, TokensDePrueba.bearer(USUARIO, "TURISTA")))
                .andExpect(status().isBadRequest());
    }

    /** Una ruta por prefijo; responde la identidad que recibió. */
    @RestController
    static class RutasDePrueba {

        @GetMapping({"/prestador/prueba", "/admin/prueba", "/turista/prueba", "/operador/prueba", "/gestor/prueba",
                "/cuenta/prueba"})
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
