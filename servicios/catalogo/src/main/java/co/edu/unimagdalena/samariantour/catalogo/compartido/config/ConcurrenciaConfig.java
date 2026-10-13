package co.edu.unimagdalena.samariantour.catalogo.compartido.config;

import co.edu.unimagdalena.samariantour.catalogo.compartido.concurrencia.EjecutorConReintento;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Reintento ante conflictos de concurrencia (ADR-05): 3 intentos con una espera aleatoria de hasta 50 ms.
 */
@Configuration
public class ConcurrenciaConfig {

    @Bean
    EjecutorConReintento ejecutorConReintento() {
        return new EjecutorConReintento();
    }
}
