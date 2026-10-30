package co.edu.unimagdalena.samariantour.nucleo.compartido.config;

import co.edu.unimagdalena.samariantour.nucleo.compartido.seguridad.Rol;
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

import java.util.Arrays;

/**
 * Autorización por prefijo de ruta, en un solo lugar para todos los módulos (DN-05, DN-37), con Spring Security y
 * {@code libs/seguridad-jwt}: cada prefijo exige un rol del token de acceso y {@code /cuenta/**} admite los cinco.
 * Sin token, 401 {@code NO_AUTENTICADO}; con otro rol, 403 {@code ACCESO_DENEGADO}.
 *
 * <p>Las demás rutas son públicas. {@code /internal/**} y {@code /actuator/**} solo se alcanzan desde la red de Docker:
 * Nginx las bloquea hacia afuera. La autenticación entre servicios queda pendiente (DN-37).
 */
@Configuration
public class SeguridadConfig {

    /** El núcleo valida sus propios tokens con la pública que sale de su clave privada: no necesita otra variable. */
    @Bean
    JwtDecoder decodificadorToken(@Value("${nucleo.jwt.clave-privada:}") String clavePrivada,
                                  @Value("${nucleo.jwt.emisor}") String emisor) {
        return DecodificadorToken.crear(ClavesRsa.publicaDe(ClavesRsa.privada(clavePrivada)), emisor);
    }

    @Bean
    SecurityFilterChain seguridad(HttpSecurity http, JwtDecoder decodificador, ObjectMapper json) throws Exception {
        return SeguridadJwt.aplicar(http, decodificador, json)
                .authorizeHttpRequests(rutas -> rutas
                        .requestMatchers("/prestador/**").hasRole(Rol.PRESTADOR.name())
                        .requestMatchers("/admin/**").hasRole(Rol.ADMINISTRADOR.name())
                        .requestMatchers("/turista/**").hasRole(Rol.TURISTA.name())
                        .requestMatchers("/operador/**").hasRole(Rol.OPERADOR_DISPONIBILIDAD.name())
                        .requestMatchers("/gestor/**").hasRole(Rol.GESTOR_DESTINO.name())
                        // Los datos de la propia cuenta, de cualquier rol (RF-12).
                        .requestMatchers("/cuenta/**").hasAnyRole(todos())
                        .anyRequest().permitAll())
                .build();
    }

    private static String[] todos() {
        return Arrays.stream(Rol.values()).map(Rol::name).toArray(String[]::new);
    }
}
