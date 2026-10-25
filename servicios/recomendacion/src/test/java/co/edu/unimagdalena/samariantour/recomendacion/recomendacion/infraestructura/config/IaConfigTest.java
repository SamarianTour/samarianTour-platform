package co.edu.unimagdalena.samariantour.recomendacion.recomendacion.infraestructura.config;

import co.edu.unimagdalena.samariantour.recomendacion.recomendacion.CatalogoDePrueba;
import co.edu.unimagdalena.samariantour.recomendacion.recomendacion.dominio.modelo.MotorRecomendacion;
import co.edu.unimagdalena.samariantour.recomendacion.recomendacion.dominio.puerto.salida.CatalogoConsultaPort;
import co.edu.unimagdalena.samariantour.recomendacion.recomendacion.dominio.puerto.salida.ExposicionPort;
import co.edu.unimagdalena.samariantour.recomendacion.recomendacion.dominio.puerto.salida.RecommendationPort;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.web.client.RestClient;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Qué motor queda como principal según la configuración (QAS-06, DR-01). No hace ninguna llamada.
 */
class IaConfigTest {

    private final ApplicationContextRunner contexto = new ApplicationContextRunner()
            .withUserConfiguration(RecomendacionConfig.class, IaConfig.class)
            .withBean(CatalogoConsultaPort.class, () -> CatalogoDePrueba.de(List.of()))
            .withBean(ExposicionPort.class, () -> registro -> { })
            .withBean(RestClient.Builder.class, RestClient::builder)
            .withBean(CircuitBreakerRegistry.class, CircuitBreakerRegistry::ofDefaults)
            .withBean(ObjectMapper.class, ObjectMapper::new);

    @Test
    @DisplayName("por defecto (IA apagada) el motor principal es el de reglas")
    void porDefecto() {
        contexto.run(c -> assertThat(c.getBean(RecommendationPort.class).motor()).isEqualTo(MotorRecomendacion.REGLAS));
    }

    @Test
    @DisplayName("habilitada y con key, el principal es el de IA")
    void conKey() {
        contexto.withPropertyValues("recomendacion.ia.habilitada=true", "recomendacion.ia.api-key=gsk_prueba")
                .run(c -> assertThat(c.getBean(RecommendationPort.class).motor()).isEqualTo(MotorRecomendacion.IA));
    }

    @Test
    @DisplayName("habilitada pero sin key, o con key pero apagada: reglas")
    void sinKeyOApagada() {
        contexto.withPropertyValues("recomendacion.ia.habilitada=true", "recomendacion.ia.api-key= ")
                .run(c -> assertThat(c.getBean(RecommendationPort.class).motor())
                        .isEqualTo(MotorRecomendacion.REGLAS));
        contexto.withPropertyValues("recomendacion.ia.habilitada=false", "recomendacion.ia.api-key=gsk_prueba")
                .run(c -> assertThat(c.getBean(RecommendationPort.class).motor())
                        .isEqualTo(MotorRecomendacion.REGLAS));
    }

    @Test
    @DisplayName("cambiar url y modelo es solo configuración: el mismo adaptador sirve para Ollama")
    void otroProveedor() {
        contexto.withPropertyValues("recomendacion.ia.habilitada=true", "recomendacion.ia.api-key=ollama",
                        "recomendacion.ia.url=http://localhost:11434/v1", "recomendacion.ia.modelo=llama3.2",
                        "recomendacion.ia.esfuerzo-razonamiento=")
                .run(c -> assertThat(c.getBean(RecommendationPort.class).motor()).isEqualTo(MotorRecomendacion.IA));
    }
}
