package co.edu.unimagdalena.samariantour.seguridadjwt;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jwt.JwtDecoder;

/**
 * La configuración de Spring Security común a los tres servicios. Cada uno agrega después sus reglas por prefijo de
 * ruta ({@code authorizeHttpRequests}):
 * <ul>
 *   <li>servidor de recursos: el token llega en {@code Authorization: Bearer} y se valida con
 *       {@link DecodificadorToken}; sin estado, así que no hay sesión del servidor, cookie ni CSRF;</li>
 *   <li>sin formulario de inicio de sesión ni autenticación básica: la sesión se abre en el núcleo
 *       ({@code POST /cuentas/sesion});</li>
 *   <li>los errores 401 y 403 los responde {@link RespuestasSeguridad};</li>
 *   <li>el cortafuegos de Spring Security queda activo: rechaza con 400 las rutas con {@code ;}, {@code %2F},
 *       {@code %25}, {@code ..} o {@code //}, que podrían saltarse las reglas por prefijo.</li>
 * </ul>
 */
public final class SeguridadJwt {

    private SeguridadJwt() {
    }

    public static HttpSecurity aplicar(HttpSecurity http, JwtDecoder decodificador, ObjectMapper json)
            throws Exception {
        RespuestasSeguridad respuestas = new RespuestasSeguridad(json);
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(sesion -> sesion.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .requestCache(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .exceptionHandling(errores -> errores
                        .authenticationEntryPoint(respuestas)
                        .accessDeniedHandler(respuestas))
                .oauth2ResourceServer(recursos -> recursos
                        .jwt(jwt -> jwt.decoder(decodificador).jwtAuthenticationConverter(AutenticacionToken::de))
                        .authenticationEntryPoint(respuestas)
                        .accessDeniedHandler(respuestas));
    }
}
