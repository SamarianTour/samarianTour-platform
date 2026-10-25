package co.edu.unimagdalena.samariantour.recomendacion.recomendacion.infraestructura.config;

import co.edu.unimagdalena.samariantour.recomendacion.recomendacion.aplicacion.ActualizarAlertasService;
import co.edu.unimagdalena.samariantour.recomendacion.recomendacion.aplicacion.ConsultarAlertasService;
import co.edu.unimagdalena.samariantour.recomendacion.recomendacion.dominio.puerto.entrada.ActualizarAlertasUseCase;
import co.edu.unimagdalena.samariantour.recomendacion.recomendacion.dominio.puerto.entrada.ConsultarAlertasZonaUseCase;
import co.edu.unimagdalena.samariantour.recomendacion.recomendacion.dominio.puerto.salida.AlertaCachePort;
import co.edu.unimagdalena.samariantour.recomendacion.recomendacion.dominio.puerto.salida.CatalogoConsultaPort;
import co.edu.unimagdalena.samariantour.recomendacion.recomendacion.dominio.puerto.salida.WeatherAlertPort;
import co.edu.unimagdalena.samariantour.recomendacion.recomendacion.infraestructura.entrada.programada.AlertasScheduler;
import co.edu.unimagdalena.samariantour.recomendacion.recomendacion.infraestructura.salida.alertas.AlertaCacheCaffeineAdapter;
import co.edu.unimagdalena.samariantour.recomendacion.recomendacion.infraestructura.salida.alertas.AlertasSimuladasAdapter;
import co.edu.unimagdalena.samariantour.recomendacion.recomendacion.infraestructura.salida.alertas.PropiedadesAlertas;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.time.Clock;

/**
 * Alertas del destino (RF-28): la fuente que diga {@code recomendacion.alertas.fuente} (DR-14), la caché con la
 * última lectura buena y su actualización programada (DR-16). La fuente del IDEAM la registra
 * {@code AlertasIdeamConfig}.
 */
@Configuration
@EnableScheduling
@EnableConfigurationProperties(PropiedadesAlertas.class)
public class AlertasConfig {

    /** Por defecto, y siempre que la fuente no sea otra conocida. */
    @Bean
    @ConditionalOnProperty(name = "recomendacion.alertas.fuente", havingValue = PropiedadesAlertas.SIMULADA,
            matchIfMissing = true)
    WeatherAlertPort alertasSimuladas(ObjectMapper json) {
        return new AlertasSimuladasAdapter(new ClassPathResource("alertas/alertas-simuladas.json"), json,
                Clock.systemUTC());
    }

    @Bean
    AlertaCachePort alertaCache() {
        return new AlertaCacheCaffeineAdapter();
    }

    @Bean
    ActualizarAlertasUseCase actualizarAlertas(WeatherAlertPort fuente, AlertaCachePort cache,
                                               CatalogoConsultaPort catalogo) {
        return new ActualizarAlertasService(fuente, cache, catalogo, Clock.systemUTC());
    }

    @Bean
    ConsultarAlertasZonaUseCase consultarAlertas(AlertaCachePort cache, CatalogoConsultaPort catalogo) {
        return new ConsultarAlertasService(cache, catalogo, Clock.systemUTC());
    }

    @Bean
    AlertasScheduler alertasScheduler(ActualizarAlertasUseCase actualizarAlertas) {
        return new AlertasScheduler(actualizarAlertas);
    }
}
