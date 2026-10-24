package co.edu.unimagdalena.samariantour.recomendacion.recomendacion.infraestructura.config;

import co.edu.unimagdalena.samariantour.recomendacion.recomendacion.aplicacion.RecomendacionService;
import co.edu.unimagdalena.samariantour.recomendacion.recomendacion.dominio.modelo.MotorRecomendacion;
import co.edu.unimagdalena.samariantour.recomendacion.recomendacion.dominio.politica.PoliticaRotacion;
import co.edu.unimagdalena.samariantour.recomendacion.recomendacion.dominio.politica.ReglaCriteriosExplicitos;
import co.edu.unimagdalena.samariantour.recomendacion.recomendacion.dominio.puerto.entrada.RecomendarOfertasUseCase;
import co.edu.unimagdalena.samariantour.recomendacion.recomendacion.dominio.puerto.salida.CatalogoConsultaPort;
import co.edu.unimagdalena.samariantour.recomendacion.recomendacion.dominio.puerto.salida.ExposicionPort;
import co.edu.unimagdalena.samariantour.recomendacion.recomendacion.dominio.puerto.salida.RecommendationPort;
import co.edu.unimagdalena.samariantour.recomendacion.recomendacion.infraestructura.salida.reglas.RecomendadorPorReglasAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import java.time.Clock;
import java.util.concurrent.ThreadLocalRandom;
import java.util.random.RandomGenerator;

/**
 * Crea el caso de uso con sus políticas y sus motores. El motor por reglas siempre existe: es el respaldo. Si la IA
 * está configurada, {@code IaConfig} registra su adaptador como el motor principal (DR-01); si no, el principal
 * también es el de reglas.
 */
@Configuration
public class RecomendacionConfig {

    private static final Logger LOG = LoggerFactory.getLogger(RecomendacionConfig.class);

    @Bean
    ReglaCriteriosExplicitos reglaCriteriosExplicitos() {
        return new ReglaCriteriosExplicitos();
    }

    /** El azar de cada hilo: el bean se comparte entre peticiones concurrentes. */
    @Bean
    PoliticaRotacion politicaRotacion() {
        RandomGenerator azarDelHilo = () -> ThreadLocalRandom.current().nextLong();
        return new PoliticaRotacion(azarDelHilo);
    }

    @Bean
    RecomendadorPorReglasAdapter recomendadorPorReglas() {
        return new RecomendadorPorReglasAdapter();
    }

    /**
     * @param motor el principal: el de IA si {@code IaConfig} lo registró ({@code @Primary}), si no el de reglas
     */
    @Bean
    RecomendarOfertasUseCase recomendarOfertas(CatalogoConsultaPort catalogo, RecommendationPort motor,
                                               RecomendadorPorReglasAdapter respaldo, ExposicionPort exposicion,
                                               ReglaCriteriosExplicitos regla, PoliticaRotacion rotacion) {
        return new RecomendacionService(catalogo, motor, respaldo, exposicion, regla, rotacion, Clock.systemUTC());
    }

    /** Deja en el log qué motor responde, sin la key. */
    @Bean
    ApplicationRunner motorAlArrancar(RecommendationPort motor, Environment entorno) {
        return argumentos -> {
            if (motor.motor() == MotorRecomendacion.REGLAS
                    && entorno.getProperty("recomendacion.ia.habilitada", Boolean.class, false)) {
                LOG.warn("recomendacion.ia.habilitada=true pero sin api-key: responde el motor por reglas");
            }
            LOG.info("motor de recomendacion principal: {}", motor.motor());
        };
    }
}
