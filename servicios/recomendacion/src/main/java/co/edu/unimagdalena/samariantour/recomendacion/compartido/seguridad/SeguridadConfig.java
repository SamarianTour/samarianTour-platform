package co.edu.unimagdalena.samariantour.recomendacion.compartido.seguridad;

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
 * Spring Security con {@code libs/seguridad-jwt}, como en los otros dos servicios (DR-22). Todas las rutas de la
 * recomendación son públicas según su contrato, así que ninguna exige rol; aun así:
 * <ul>
 *   <li>un token que llegue se valida con la clave pública del núcleo, y si no sirve responde 401;</li>
 *   <li>el cortafuegos de Spring Security rechaza las rutas con {@code ;} o {@code ..}, que intentan esquivar a
 *       Nginx;</li>
 *   <li>cuando una ruta necesite al turista (el registro de exposición de RET-02), basta con agregar aquí su
 *       regla.</li>
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
    SecurityFilterChain seguridad(HttpSecurity http, JwtDecoder decodificador, ObjectMapper json) throws Exception {
        return SeguridadJwt.aplicar(http, decodificador, json)
                .authorizeHttpRequests(rutas -> rutas.anyRequest().permitAll())
                .build();
    }
}
