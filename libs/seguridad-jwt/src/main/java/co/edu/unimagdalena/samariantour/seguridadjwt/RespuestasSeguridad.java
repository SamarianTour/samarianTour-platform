package co.edu.unimagdalena.samariantour.seguridadjwt;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Los errores de seguridad en el mismo formato que los demás errores de los servicios: ProblemDetail (RFC 7807) con
 * la propiedad {@code codigo}. Sin token o con uno que no sirve, 401 {@code NO_AUTENTICADO} con
 * {@code WWW-Authenticate: Bearer} (RFC 6750); con un rol que la ruta no admite, 403 {@code ACCESO_DENEGADO}. El
 * detalle no dice por qué se rechazó el token, para no orientar a quien intenta falsificarlo.
 */
public final class RespuestasSeguridad implements AuthenticationEntryPoint, AccessDeniedHandler {

    public static final String NO_AUTENTICADO = "NO_AUTENTICADO";
    public static final String ACCESO_DENEGADO = "ACCESO_DENEGADO";

    private final ObjectMapper json;

    public RespuestasSeguridad(ObjectMapper json) {
        this.json = json;
    }

    @Override
    public void commence(HttpServletRequest peticion, HttpServletResponse respuesta, AuthenticationException error)
            throws IOException {
        boolean conToken = peticion.getHeader(HttpHeaders.AUTHORIZATION) != null;
        respuesta.setHeader(HttpHeaders.WWW_AUTHENTICATE, conToken ? "Bearer error=\"invalid_token\"" : "Bearer");
        escribir(peticion, respuesta, HttpStatus.UNAUTHORIZED, "No autenticado", NO_AUTENTICADO, conToken
                ? "El token de acceso no es válido o ya venció; inicia sesión o renueva la sesión"
                : "Esta ruta exige iniciar sesión: envía el token de acceso en Authorization: Bearer");
    }

    @Override
    public void handle(HttpServletRequest peticion, HttpServletResponse respuesta, AccessDeniedException error)
            throws IOException {
        escribir(peticion, respuesta, HttpStatus.FORBIDDEN, "Acceso denegado", ACCESO_DENEGADO,
                "Tu rol no puede usar esta ruta");
    }

    private void escribir(HttpServletRequest peticion, HttpServletResponse respuesta, HttpStatus estado, String titulo,
                          String codigo, String detalle) throws IOException {
        Map<String, Object> problema = new LinkedHashMap<>();
        problema.put("type", "about:blank");
        problema.put("title", titulo);
        problema.put("status", estado.value());
        problema.put("detail", detalle);
        problema.put("instance", peticion.getRequestURI());
        problema.put("codigo", codigo);
        respuesta.setStatus(estado.value());
        // Como los ProblemDetail de los controladores: sin charset, porque Jackson escribe los bytes en UTF-8.
        respuesta.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        json.writeValue(respuesta.getOutputStream(), problema);
    }
}
