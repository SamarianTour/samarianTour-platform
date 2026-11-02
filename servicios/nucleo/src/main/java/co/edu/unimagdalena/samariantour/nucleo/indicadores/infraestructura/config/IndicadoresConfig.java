package co.edu.unimagdalena.samariantour.nucleo.indicadores.infraestructura.config;

import co.edu.unimagdalena.samariantour.nucleo.indicadores.aplicacion.IndicadoresService;
import co.edu.unimagdalena.samariantour.nucleo.indicadores.dominio.puerto.salida.SolicitudesPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

/** Crea los casos de uso de los indicadores y los conecta con sus adaptadores. */
@Configuration
public class IndicadoresConfig {

    /** Implementa el tablero (RF-85, RF-86), el embudo (RF-87, RF-88) y la distribución por categoría (RF-89). */
    @Bean
    IndicadoresService indicadores(SolicitudesPort solicitudes, Clock reloj) {
        return new IndicadoresService(solicitudes, reloj);
    }
}
