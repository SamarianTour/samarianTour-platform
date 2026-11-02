package co.edu.unimagdalena.samariantour.nucleo.indicadores.infraestructura.config;

import co.edu.unimagdalena.samariantour.nucleo.indicadores.aplicacion.ConsultarAlertasSesgoService;
import co.edu.unimagdalena.samariantour.nucleo.indicadores.aplicacion.EvaluarIndiceExposicionService;
import co.edu.unimagdalena.samariantour.nucleo.indicadores.aplicacion.IndicadoresService;
import co.edu.unimagdalena.samariantour.nucleo.indicadores.aplicacion.RegistrarExposicionService;
import co.edu.unimagdalena.samariantour.nucleo.indicadores.dominio.puerto.entrada.ConsultarAlertasSesgoUseCase;
import co.edu.unimagdalena.samariantour.nucleo.indicadores.dominio.puerto.entrada.EvaluarIndiceExposicionUseCase;
import co.edu.unimagdalena.samariantour.nucleo.indicadores.dominio.puerto.entrada.RegistrarExposicionUseCase;
import co.edu.unimagdalena.samariantour.nucleo.indicadores.dominio.puerto.salida.AlertaSesgoRepositoryPort;
import co.edu.unimagdalena.samariantour.nucleo.indicadores.dominio.puerto.salida.ExposicionRepositoryPort;
import co.edu.unimagdalena.samariantour.nucleo.indicadores.dominio.puerto.salida.OfertaElegibleConsultaPort;
import co.edu.unimagdalena.samariantour.nucleo.indicadores.dominio.puerto.salida.SolicitudesPort;
import co.edu.unimagdalena.samariantour.nucleo.indicadores.dominio.puerto.salida.TransaccionPort;
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

    @Bean
    RegistrarExposicionUseCase registrarExposicion(ExposicionRepositoryPort exposiciones) {
        return new RegistrarExposicionService(exposiciones);
    }

    @Bean
    EvaluarIndiceExposicionUseCase evaluarIndiceExposicion(ExposicionRepositoryPort exposiciones,
                                                           AlertaSesgoRepositoryPort alertas,
                                                           OfertaElegibleConsultaPort catalogo,
                                                           TransaccionPort transaccion, Clock reloj) {
        return new EvaluarIndiceExposicionService(exposiciones, alertas, catalogo, transaccion, reloj);
    }

    @Bean
    ConsultarAlertasSesgoUseCase consultarAlertasSesgo(AlertaSesgoRepositoryPort alertas) {
        return new ConsultarAlertasSesgoService(alertas);
    }
}
