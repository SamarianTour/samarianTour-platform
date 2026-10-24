package co.edu.unimagdalena.samariantour.nucleo.administracion.infraestructura.config;

import co.edu.unimagdalena.samariantour.nucleo.administracion.aplicacion.ModerarOfertaService;
import co.edu.unimagdalena.samariantour.nucleo.administracion.aplicacion.ModerarOpinionService;
import co.edu.unimagdalena.samariantour.nucleo.administracion.dominio.puerto.entrada.ModerarOfertaUseCase;
import co.edu.unimagdalena.samariantour.nucleo.administracion.dominio.puerto.entrada.ModerarOpinionUseCase;
import co.edu.unimagdalena.samariantour.nucleo.administracion.dominio.puerto.salida.AvisosModeracionPort;
import co.edu.unimagdalena.samariantour.nucleo.administracion.dominio.puerto.salida.DecisionesModeracionRepositoryPort;
import co.edu.unimagdalena.samariantour.nucleo.administracion.dominio.puerto.salida.ModeracionCatalogoPort;
import co.edu.unimagdalena.samariantour.nucleo.administracion.dominio.puerto.salida.NotificacionPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

/** Crea los casos de uso de la administración y los conecta con sus adaptadores. */
@Configuration
public class AdministracionConfig {

    @Bean
    ModerarOfertaUseCase moderarOferta(DecisionesModeracionRepositoryPort decisiones, ModeracionCatalogoPort catalogo,
                                       AvisosModeracionPort avisos, NotificacionPort notificacion, Clock reloj) {
        return new ModerarOfertaService(decisiones, catalogo, avisos, notificacion, reloj);
    }

    @Bean
    ModerarOpinionUseCase moderarOpinion(DecisionesModeracionRepositoryPort decisiones,
                                         ModeracionCatalogoPort catalogo, AvisosModeracionPort avisos,
                                         NotificacionPort notificacion, Clock reloj) {
        return new ModerarOpinionService(decisiones, catalogo, avisos, notificacion, reloj);
    }
}
