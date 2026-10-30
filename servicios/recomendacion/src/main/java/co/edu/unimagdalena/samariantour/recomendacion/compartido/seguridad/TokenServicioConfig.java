package co.edu.unimagdalena.samariantour.recomendacion.compartido.seguridad;

import co.edu.unimagdalena.samariantour.seguridadjwt.ClavesRsa;
import co.edu.unimagdalena.samariantour.seguridadjwt.EmisorTokenServicio;
import co.edu.unimagdalena.samariantour.seguridadjwt.ServiciosInternos;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

/**
 * El token con el que la recomendación lee las ofertas visibles en {@code /internal/ofertas} del catálogo (DN-38,
 * DR-23).
 * Lo firma con su propia clave privada ({@code RECOMENDACION_JWT_CLAVE_PRIVADA}), nunca con la del núcleo: si se
 * filtrara, solo serviría para lo que el catálogo le permite a la recomendación.
 */
@Configuration
public class TokenServicioConfig {

    @Bean
    EmisorTokenServicio emisorTokenServicio(@Value("${recomendacion.jwt.clave-privada:}") String clavePrivada,
                                            @Value("${recomendacion.jwt.emisor}") String emisor) {
        return new EmisorTokenServicio(ServiciosInternos.RECOMENDACION, emisor,
                ClavesRsa.privada(clavePrivada, "RECOMENDACION_JWT_CLAVE_PRIVADA"), Clock.systemUTC());
    }
}
