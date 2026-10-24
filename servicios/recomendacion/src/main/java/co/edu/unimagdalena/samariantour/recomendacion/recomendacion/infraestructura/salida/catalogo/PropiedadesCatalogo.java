package co.edu.unimagdalena.samariantour.recomendacion.recomendacion.infraestructura.salida.catalogo;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.time.Duration;

/**
 * Configuración del cliente del catálogo ({@code recomendacion.catalogo.*}).
 *
 * @param url           base del catálogo, con su context path; en Docker Compose, http://catalogo:8080/api/catalogo
 * @param timeout       corte por petición (diagrama del módulo 09: 3 s)
 * @param vigenciaCache cada cuánto se vuelve a leer el catálogo [SUPUESTO] (DR-07)
 */
@ConfigurationProperties("recomendacion.catalogo")
public record PropiedadesCatalogo(@DefaultValue("http://localhost:8080/api/catalogo") String url,
                                  @DefaultValue("3s") Duration timeout,
                                  @DefaultValue("60s") Duration vigenciaCache) {
}
