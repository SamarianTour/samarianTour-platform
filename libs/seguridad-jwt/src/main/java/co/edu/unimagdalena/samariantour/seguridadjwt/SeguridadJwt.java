package co.edu.unimagdalena.samariantour.seguridadjwt;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.util.List;

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
 *
 * <p>Las rutas internas ({@code /internal/**}) tienen su propia cadena, {@link #aplicarServicios}, que solo acepta el
 * token de otro servicio (DN-38).
 */
public final class SeguridadJwt {

    /** Prefijo de la autoridad del servicio que llama: {@code SERVICIO_nucleo}, {@code SERVICIO_recomendacion}. */
    public static final String PREFIJO_SERVICIO = "SERVICIO_";

    private SeguridadJwt() {
    }

    /** La cadena de las rutas de los usuarios, con el token de acceso del núcleo. */
    public static HttpSecurity aplicar(HttpSecurity http, JwtDecoder decodificador, ObjectMapper json)
            throws Exception {
        RespuestasSeguridad respuestas = new RespuestasSeguridad(json);
        return comun(http, respuestas)
                .oauth2ResourceServer(recursos -> recursos
                        .jwt(jwt -> jwt.decoder(decodificador).jwtAuthenticationConverter(AutenticacionToken::de))
                        .authenticationEntryPoint(respuestas)
                        .accessDeniedHandler(respuestas));
    }

    /**
     * La cadena de {@code /internal/**}, con el token de servicio que valida {@link DecodificadorTokenServicio}. Quien
     * llama queda con la autoridad {@code SERVICIO_<nombre>}: cada servicio la exige en las rutas internas que le toca
     * atender ({@link #servicio}). Va antes que la cadena de los usuarios ({@code @Order}).
     */
    public static HttpSecurity aplicarServicios(HttpSecurity http, DecodificadorTokenServicio decodificador,
                                                ObjectMapper json) throws Exception {
        RespuestasSeguridad respuestas = new RespuestasSeguridad(json);
        return comun(http.securityMatcher("/internal/**"), respuestas)
                .oauth2ResourceServer(recursos -> recursos
                        .jwt(jwt -> jwt.decoder(decodificador).jwtAuthenticationConverter(token ->
                                new JwtAuthenticationToken(token, List.of(new SimpleGrantedAuthority(
                                        servicio(token.getSubject()))), token.getSubject())))
                        .authenticationEntryPoint(respuestas)
                        .accessDeniedHandler(respuestas));
    }

    /** La autoridad del servicio {@code nombre}, para las reglas {@code hasAuthority}. */
    public static String servicio(String nombre) {
        return PREFIJO_SERVICIO + nombre;
    }

    private static HttpSecurity comun(HttpSecurity http, RespuestasSeguridad respuestas) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(sesion -> sesion.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .requestCache(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .exceptionHandling(errores -> errores
                        .authenticationEntryPoint(respuestas)
                        .accessDeniedHandler(respuestas));
    }
}
