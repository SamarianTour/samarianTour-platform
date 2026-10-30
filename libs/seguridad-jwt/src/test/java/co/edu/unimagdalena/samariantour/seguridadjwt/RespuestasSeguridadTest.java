package co.edu.unimagdalena.samariantour.seguridadjwt;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.InsufficientAuthenticationException;

import static org.assertj.core.api.Assertions.assertThat;

class RespuestasSeguridadTest {

    private final ObjectMapper json = new ObjectMapper();
    private final RespuestasSeguridad respuestas = new RespuestasSeguridad(json);

    @Test
    @DisplayName("sin token: 401 NO_AUTENTICADO, ProblemDetail y WWW-Authenticate: Bearer")
    void sinToken() throws Exception {
        MockHttpServletResponse respuesta = new MockHttpServletResponse();
        respuestas.commence(peticion(), respuesta, new InsufficientAuthenticationException("sin token"));

        assertThat(respuesta.getStatus()).isEqualTo(401);
        assertThat(respuesta.getHeader("WWW-Authenticate")).isEqualTo("Bearer");
        assertThat(respuesta.getContentType()).startsWith("application/problem+json");
        JsonNode problema = json.readTree(respuesta.getContentAsByteArray());
        assertThat(problema.get("codigo").asText()).isEqualTo("NO_AUTENTICADO");
        assertThat(problema.get("status").asInt()).isEqualTo(401);
        assertThat(problema.get("instance").asText()).isEqualTo("/api/nucleo/turista/solicitudes");
    }

    @Test
    @DisplayName("con un token que no sirve: 401 con invalid_token, sin decir por qué se rechazó")
    void tokenInvalido() throws Exception {
        MockHttpServletRequest peticion = peticion();
        peticion.addHeader("Authorization", "Bearer abc");
        MockHttpServletResponse respuesta = new MockHttpServletResponse();
        respuestas.commence(peticion, respuesta, new InsufficientAuthenticationException("firma inválida"));

        assertThat(respuesta.getHeader("WWW-Authenticate")).isEqualTo("Bearer error=\"invalid_token\"");
        assertThat(respuesta.getContentAsString()).doesNotContain("firma");
    }

    @Test
    @DisplayName("con otro rol: 403 ACCESO_DENEGADO")
    void otroRol() throws Exception {
        MockHttpServletResponse respuesta = new MockHttpServletResponse();
        respuestas.handle(peticion(), respuesta, new AccessDeniedException("rol"));

        assertThat(respuesta.getStatus()).isEqualTo(403);
        assertThat(json.readTree(respuesta.getContentAsByteArray()).get("codigo").asText())
                .isEqualTo("ACCESO_DENEGADO");
    }

    private static MockHttpServletRequest peticion() {
        return new MockHttpServletRequest("GET", "/api/nucleo/turista/solicitudes");
    }
}
