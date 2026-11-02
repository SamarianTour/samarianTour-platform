package co.edu.unimagdalena.samariantour.nucleo.administracion.infraestructura.config;

import co.edu.unimagdalena.samariantour.nucleo.administracion.aplicacion.AsignarOperadorPrestadorService;
import co.edu.unimagdalena.samariantour.nucleo.administracion.aplicacion.GestionarCuentasInternasService;
import co.edu.unimagdalena.samariantour.nucleo.administracion.aplicacion.ModerarOfertaService;
import co.edu.unimagdalena.samariantour.nucleo.administracion.aplicacion.ModerarOpinionService;
import co.edu.unimagdalena.samariantour.nucleo.administracion.aplicacion.SincronizarRntAdminService;
import co.edu.unimagdalena.samariantour.nucleo.administracion.dominio.puerto.entrada.AsignarOperadorPrestadorUseCase;
import co.edu.unimagdalena.samariantour.nucleo.administracion.dominio.puerto.entrada.GestionarCuentasInternasUseCase;
import co.edu.unimagdalena.samariantour.nucleo.administracion.dominio.puerto.entrada.ModerarOfertaUseCase;
import co.edu.unimagdalena.samariantour.nucleo.administracion.dominio.puerto.entrada.ModerarOpinionUseCase;
import co.edu.unimagdalena.samariantour.nucleo.administracion.dominio.puerto.entrada.SincronizarRntAdminUseCase;
import co.edu.unimagdalena.samariantour.nucleo.administracion.dominio.puerto.salida.AvisosModeracionPort;
import co.edu.unimagdalena.samariantour.nucleo.administracion.dominio.puerto.salida.CuentasInternasPort;
import co.edu.unimagdalena.samariantour.nucleo.administracion.dominio.puerto.salida.DecisionesModeracionRepositoryPort;
import co.edu.unimagdalena.samariantour.nucleo.administracion.dominio.puerto.salida.ModeracionCatalogoPort;
import co.edu.unimagdalena.samariantour.nucleo.administracion.dominio.puerto.salida.NotificacionPort;
import co.edu.unimagdalena.samariantour.nucleo.administracion.dominio.puerto.salida.OperadoresPrestadoresPort;
import co.edu.unimagdalena.samariantour.nucleo.administracion.dominio.puerto.salida.PrestadoresModeracionPort;
import co.edu.unimagdalena.samariantour.nucleo.administracion.dominio.puerto.salida.SincronizacionRntPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

/** Crea los casos de uso de la administración y los conecta con sus adaptadores. */
@Configuration
public class AdministracionConfig {

    @Bean
    ModerarOfertaUseCase moderarOferta(DecisionesModeracionRepositoryPort decisiones, ModeracionCatalogoPort catalogo,
                                       AvisosModeracionPort avisos, NotificacionPort notificacion,
                                       PrestadoresModeracionPort prestadores, Clock reloj) {
        return new ModerarOfertaService(decisiones, catalogo, avisos, notificacion, prestadores, reloj);
    }

    @Bean
    ModerarOpinionUseCase moderarOpinion(DecisionesModeracionRepositoryPort decisiones,
                                         ModeracionCatalogoPort catalogo, AvisosModeracionPort avisos,
                                         NotificacionPort notificacion, Clock reloj) {
        return new ModerarOpinionService(decisiones, catalogo, avisos, notificacion, reloj);
    }

    @Bean
    SincronizarRntAdminUseCase sincronizarRntAdmin(SincronizacionRntPort prestadores) {
        return new SincronizarRntAdminService(prestadores);
    }

    @Bean
    AsignarOperadorPrestadorUseCase asignarOperadorPrestador(CuentasInternasPort cuentas,
                                                             OperadoresPrestadoresPort prestadores) {
        return new AsignarOperadorPrestadorService(cuentas, prestadores);
    }

    @Bean
    GestionarCuentasInternasUseCase gestionarCuentasInternas(CuentasInternasPort cuentas,
                                                             OperadoresPrestadoresPort prestadores) {
        return new GestionarCuentasInternasService(cuentas, prestadores);
    }
}
