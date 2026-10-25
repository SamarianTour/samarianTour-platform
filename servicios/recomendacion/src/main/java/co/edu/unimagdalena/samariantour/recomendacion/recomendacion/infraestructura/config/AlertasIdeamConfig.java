package co.edu.unimagdalena.samariantour.recomendacion.recomendacion.infraestructura.config;

import co.edu.unimagdalena.samariantour.recomendacion.compartido.http.ClientesHttp;
import co.edu.unimagdalena.samariantour.recomendacion.recomendacion.dominio.puerto.salida.WeatherAlertPort;
import co.edu.unimagdalena.samariantour.recomendacion.recomendacion.infraestructura.salida.alertas.IdeamAlertasHidrologicasAdapter;
import co.edu.unimagdalena.samariantour.recomendacion.recomendacion.infraestructura.salida.alertas.PropiedadesAlertas;
import co.edu.unimagdalena.samariantour.recomendacion.recomendacion.infraestructura.salida.alertas.PropiedadesIdeam;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

/**
 * La fuente de alertas del IDEAM, solo con {@code recomendacion.alertas.fuente=ideam} (DR-14). Usa el mismo corte de
 * circuito que el proveedor de IA, en su propia instancia ({@code resilience4j.circuitbreaker.instances.alertas}).
 */
@Configuration
@EnableConfigurationProperties(PropiedadesIdeam.class)
public class AlertasIdeamConfig {

    @Bean
    @ConditionalOnProperty(name = "recomendacion.alertas.fuente", havingValue = PropiedadesAlertas.IDEAM)
    WeatherAlertPort alertasIdeam(PropiedadesIdeam propiedades, RestClient.Builder clientes,
                                  CircuitBreakerRegistry circuitos) {
        RestClient ideam = clientes.baseUrl(propiedades.url())
                .requestFactory(ClientesHttp.conTiempoMaximo(propiedades.timeout()))
                .build();
        return new IdeamAlertasHidrologicasAdapter(ideam, circuitos.circuitBreaker("alertas"));
    }
}
