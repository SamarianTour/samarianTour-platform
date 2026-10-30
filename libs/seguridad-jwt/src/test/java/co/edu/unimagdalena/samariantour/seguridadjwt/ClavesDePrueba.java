package co.edu.unimagdalena.samariantour.seguridadjwt;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.util.Map;

/**
 * Entrega las claves de {@link TokensDePrueba} a todo contexto de Spring que arranque en las pruebas, también a los de
 * {@code @WebMvcTest}: {@code seguridad.jwt.clave-publica} para validar y {@code seguridad.jwt.pruebas.clave-privada}
 * para que el núcleo firme con la misma pareja, y la pareja de la recomendación para sus tokens de servicio (DN-38).
 * Está registrado en {@code META-INF/spring.factories} del test-jar, así que solo existe en las pruebas.
 */
public class ClavesDePrueba implements EnvironmentPostProcessor {

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment entorno, SpringApplication aplicacion) {
        entorno.getPropertySources().addFirst(new MapPropertySource("seguridad-jwt-pruebas", Map.of(
                "seguridad.jwt.clave-publica", TokensDePrueba.CLAVE_PUBLICA,
                "seguridad.jwt.pruebas.clave-privada", TokensDePrueba.CLAVE_PRIVADA,
                "seguridad.servicios.recomendacion.clave-publica", TokensDePrueba.CLAVE_PUBLICA_RECOMENDACION,
                "seguridad.jwt.pruebas.clave-privada-recomendacion", TokensDePrueba.CLAVE_PRIVADA_RECOMENDACION)));
    }
}
