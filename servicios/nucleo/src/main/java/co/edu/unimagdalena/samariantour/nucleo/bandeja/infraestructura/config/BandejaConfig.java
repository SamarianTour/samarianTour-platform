package co.edu.unimagdalena.samariantour.nucleo.bandeja.infraestructura.config;

import co.edu.unimagdalena.samariantour.nucleo.bandeja.aplicacion.AdministrarBandejaService;
import co.edu.unimagdalena.samariantour.nucleo.bandeja.aplicacion.EncolarAvisoService;
import co.edu.unimagdalena.samariantour.nucleo.bandeja.aplicacion.PublicarAvisosService;
import co.edu.unimagdalena.samariantour.nucleo.bandeja.dominio.puerto.entrada.EncolarAvisoUseCase;
import co.edu.unimagdalena.samariantour.nucleo.bandeja.dominio.puerto.entrada.PublicarAvisosUseCase;
import co.edu.unimagdalena.samariantour.nucleo.bandeja.dominio.puerto.salida.BandejaSalidaRepositoryPort;
import co.edu.unimagdalena.samariantour.nucleo.bandeja.dominio.puerto.salida.CatalogoAvisosPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

/** Crea los casos de uso de la bandeja de salida (DN-06). El cliente hacia el catálogo está en compartido. */
@Configuration
public class BandejaConfig {

    @Bean
    EncolarAvisoUseCase encolarAviso(BandejaSalidaRepositoryPort bandeja, Clock reloj) {
        return new EncolarAvisoService(bandeja, reloj);
    }

    /** Un solo servicio para los dos casos de uso del administrador y de la limpieza (DN-44). */
    @Bean
    AdministrarBandejaService administrarBandeja(BandejaSalidaRepositoryPort bandeja, Clock reloj) {
        return new AdministrarBandejaService(bandeja, reloj);
    }

    @Bean
    PublicarAvisosUseCase publicarAvisos(BandejaSalidaRepositoryPort bandeja, CatalogoAvisosPort catalogo,
                                         Clock reloj) {
        return new PublicarAvisosService(bandeja, catalogo, reloj);
    }
}
