package co.edu.unimagdalena.samariantour.catalogo.compartido.config;

import co.edu.unimagdalena.samariantour.catalogo.compartido.seguridad.Rol;
import co.edu.unimagdalena.samariantour.seguridadjwt.ClavesRsa;
import co.edu.unimagdalena.samariantour.seguridadjwt.DecodificadorToken;
import co.edu.unimagdalena.samariantour.seguridadjwt.DecodificadorTokenServicio;
import co.edu.unimagdalena.samariantour.seguridadjwt.DecodificadorTokenServicio.Llamador;
import co.edu.unimagdalena.samariantour.seguridadjwt.SeguridadJwt;
import co.edu.unimagdalena.samariantour.seguridadjwt.ServiciosInternos;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.web.SecurityFilterChain;

import java.util.List;

/**
 * Autorización por prefijo de ruta, en un solo lugar para todos los módulos (DC-13), con Spring Security y
 * {@code libs/seguridad-jwt}: cada prefijo exige un rol del token de acceso del núcleo, que se valida aquí mismo con
 * su clave pública, sin consultar al núcleo (DC-01). Sin token, 401 {@code NO_AUTENTICADO}; con otro rol, 403
 * {@code ACCESO_DENEGADO}.
 *
 * <p>Las demás rutas (búsqueda, ficha, zonas, fotos) son públicas. {@code /actuator/**} solo se alcanza desde la red
 * de Docker: Nginx la bloquea hacia afuera.
 *
 * <p>{@code /internal/**} tiene su propia cadena, que va primero: además de que Nginx la bloquea, exige el token de
 * servicio de quien llama (DN-38), y cada ruta solo admite al servicio que la usa. El token de un usuario no sirve.
 * <ul>
 *   <li>el núcleo: los avisos ({@code /internal/avisos}, {@code /internal/opiniones/avisos}), la oferta de una
 *       solicitud ({@code /internal/ofertas/{id}}) y lo que va a moderar ({@code /internal/moderacion/**});</li>
 *   <li>la recomendación: las ofertas visibles ({@code GET /internal/ofertas}).</li>
 * </ul>
 */
@Configuration
public class SeguridadConfig {

    @Bean
    JwtDecoder decodificadorToken(@Value("${seguridad.jwt.clave-publica:}") String clavePublica,
                                  @Value("${seguridad.jwt.emisor}") String emisor) {
        return DecodificadorToken.crear(ClavesRsa.publica(clavePublica), emisor);
    }

    @Bean
    @Order(1)
    SecurityFilterChain seguridadInterna(HttpSecurity http, ObjectMapper json,
                                         @Value("${seguridad.jwt.clave-publica:}") String claveNucleo,
                                         @Value("${seguridad.jwt.emisor}") String emisorNucleo,
                                         @Value("${seguridad.servicios.recomendacion.clave-publica:}")
                                         String claveRecomendacion,
                                         @Value("${seguridad.servicios.recomendacion.emisor}")
                                         String emisorRecomendacion)
            throws Exception {
        DecodificadorTokenServicio servicios = new DecodificadorTokenServicio(ServiciosInternos.CATALOGO, List.of(
                new Llamador(ServiciosInternos.NUCLEO, emisorNucleo, ClavesRsa.publica(claveNucleo)),
                new Llamador(ServiciosInternos.RECOMENDACION, emisorRecomendacion,
                        ClavesRsa.publica(claveRecomendacion, "SEGURIDAD_SERVICIOS_RECOMENDACION_CLAVE_PUBLICA"))));
        String nucleo = SeguridadJwt.servicio(ServiciosInternos.NUCLEO);
        return SeguridadJwt.aplicarServicios(http, servicios, json)
                .authorizeHttpRequests(rutas -> rutas
                        .requestMatchers(HttpMethod.GET, "/internal/ofertas")
                        .hasAuthority(SeguridadJwt.servicio(ServiciosInternos.RECOMENDACION))
                        .requestMatchers(HttpMethod.GET, "/internal/ofertas/*").hasAuthority(nucleo)
                        .requestMatchers(HttpMethod.POST, "/internal/avisos", "/internal/opiniones/avisos")
                        .hasAuthority(nucleo)
                        .requestMatchers(HttpMethod.GET, "/internal/moderacion/**").hasAuthority(nucleo)
                        .anyRequest().denyAll())
                .build();
    }

    @Bean
    @Order(2)
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
