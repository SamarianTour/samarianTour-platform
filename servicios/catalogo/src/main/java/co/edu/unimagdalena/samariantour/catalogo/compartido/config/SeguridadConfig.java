package co.edu.unimagdalena.samariantour.catalogo.compartido.config;

import co.edu.unimagdalena.samariantour.catalogo.compartido.seguridad.Rol;
import co.edu.unimagdalena.samariantour.seguridadjwt.ClavesRsa;
import co.edu.unimagdalena.samariantour.seguridadjwt.DecodificadorToken;
import co.edu.unimagdalena.samariantour.seguridadjwt.SeguridadJwt;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Autorización por prefijo de ruta, en un solo lugar para todos los módulos (DC-13), con Spring Security y
 * {@code libs/seguridad-jwt}: cada prefijo exige un rol del token de acceso del núcleo, que se valida aquí mismo con
 * su clave pública, sin consultar al núcleo (DC-01). Sin token, 401 {@code NO_AUTENTICADO}; con otro rol, 403
 * {@code ACCESO_DENEGADO}.
 *
 * <p>Las demás rutas (búsqueda, ficha, zonas, fotos) son públicas. {@code /internal/**} y {@code /actuator/**} solo se
 * alcanzan desde la red de Docker: Nginx las bloquea hacia afuera. La autenticación entre servicios queda pendiente.
 */
@Configuration
public class SeguridadConfig {

    @Bean
    JwtDecoder decodificadorToken(@Value("${seguridad.jwt.clave-publica:}") String clavePublica,
                                  @Value("${seguridad.jwt.emisor}") String emisor) {
        return DecodificadorToken.crear(ClavesRsa.publica(clavePublica), emisor);
    }

    @Bean
    SecurityFilterChain seguridad(HttpSecurity http, JwtDecoder decodificador, ObjectMapper json) throws Exception {
        return SeguridadJwt.aplicar(http, decodificador, json)
                .authorizeHttpRequests(rutas -> rutas
                        .requestMatchers("/prestador/**").hasRole(Rol.PRESTADOR.name())
                        .requestMatchers("/admin/**").hasRole(Rol.ADMINISTRADOR.name())
                        .requestMatchers("/turista/**").hasRole(Rol.TURISTA.name())
                        .anyRequest().permitAll())
                .build();
    }
}
