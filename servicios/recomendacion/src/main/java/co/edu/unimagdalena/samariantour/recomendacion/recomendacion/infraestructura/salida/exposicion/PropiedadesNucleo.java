package co.edu.unimagdalena.samariantour.recomendacion.recomendacion.infraestructura.salida.exposicion;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.time.Duration;

/**
 * Configuración del cliente del núcleo ({@code recomendacion.nucleo.*}), que recibe el registro de exposición (DR-24).
 *
 * @param url     base del núcleo, con su context path; en Docker Compose, http://nucleo:8080/api/nucleo
 * @param timeout corte por petición [SUPUESTO]: corto, porque el turista espera su recomendación mientras tanto
 */
@ConfigurationProperties("recomendacion.nucleo")
public record PropiedadesNucleo(@DefaultValue("http://localhost:8080/api/nucleo") String url,
                                @DefaultValue("1s") Duration timeout) {
}
