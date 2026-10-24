package co.edu.unimagdalena.samariantour.recomendacion.recomendacion.infraestructura.config;

import co.edu.unimagdalena.samariantour.recomendacion.compartido.http.ClientesHttp;
import co.edu.unimagdalena.samariantour.recomendacion.recomendacion.dominio.politica.PoliticaRotacion;
import co.edu.unimagdalena.samariantour.recomendacion.recomendacion.dominio.politica.ReglaCriteriosExplicitos;
import co.edu.unimagdalena.samariantour.recomendacion.recomendacion.dominio.politica.ValidacionRespuestaIA;
import co.edu.unimagdalena.samariantour.recomendacion.recomendacion.dominio.puerto.salida.RecommendationPort;
import co.edu.unimagdalena.samariantour.recomendacion.recomendacion.infraestructura.salida.ia.PropiedadesIA;
import co.edu.unimagdalena.samariantour.recomendacion.recomendacion.infraestructura.salida.ia.ProveedorIAAdapter;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.http.HttpHeaders;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

/**
 * El motor con IA, solo si {@code recomendacion.ia.habilitada=true} y hay {@code recomendacion.ia.api-key}: entonces
 * es el motor principal ({@code @Primary}). Si no, el principal es el de reglas (DR-01). El corte de circuito se
 * configura en {@code resilience4j.circuitbreaker.instances.ia} (DR-06).
 */
@Configuration
@EnableConfigurationProperties(PropiedadesIA.class)
public class IaConfig {

    @Bean
    @Primary
    @Conditional(IaConfigurada.class)
    RecommendationPort proveedorIA(PropiedadesIA propiedades, RestClient.Builder clientes,
                                   CircuitBreakerRegistry circuitos, ObjectMapper json,
                                   ReglaCriteriosExplicitos regla, PoliticaRotacion rotacion) {
        RestClient proveedor = clientes.baseUrl(propiedades.url())
                .requestFactory(ClientesHttp.conTiempoMaximo(propiedades.timeout()))
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + propiedades.apiKey())
                .build();
        return new ProveedorIAAdapter(proveedor, propiedades, circuitos.circuitBreaker("ia"), json,
                new ValidacionRespuestaIA(regla), rotacion);
    }

    /** Habilitada y con key. */
    static class IaConfigurada implements Condition {

        @Override
        public boolean matches(ConditionContext contexto, AnnotatedTypeMetadata metadatos) {
            return contexto.getEnvironment().getProperty("recomendacion.ia.habilitada", Boolean.class, false)
                    && StringUtils.hasText(contexto.getEnvironment().getProperty("recomendacion.ia.api-key"));
        }
    }
}
