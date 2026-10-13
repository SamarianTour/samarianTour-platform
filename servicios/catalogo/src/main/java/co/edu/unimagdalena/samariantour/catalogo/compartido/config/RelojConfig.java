package co.edu.unimagdalena.samariantour.catalogo.compartido.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.ZoneId;

/**
 * Reloj del servicio, en la zona horaria de Colombia. Los casos de uso lo reciben para que las pruebas
 * puedan fijar la hora.
 */
@Configuration
public class RelojConfig {

    public static final ZoneId ZONA_HORARIA = ZoneId.of("America/Bogota");

    @Bean
    Clock reloj() {
        return Clock.system(ZONA_HORARIA);
    }
}
