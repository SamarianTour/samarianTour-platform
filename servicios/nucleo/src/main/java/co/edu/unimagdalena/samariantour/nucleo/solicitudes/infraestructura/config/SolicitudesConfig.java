package co.edu.unimagdalena.samariantour.nucleo.solicitudes.infraestructura.config;

import co.edu.unimagdalena.samariantour.nucleo.compartido.concurrencia.EjecutorConReintento;
import co.edu.unimagdalena.samariantour.nucleo.solicitudes.aplicacion.AtenderSolicitudService;
import co.edu.unimagdalena.samariantour.nucleo.solicitudes.aplicacion.ConsultarColaOperadorService;
import co.edu.unimagdalena.samariantour.nucleo.solicitudes.aplicacion.ConsultarSolicitudesTuristaService;
import co.edu.unimagdalena.samariantour.nucleo.solicitudes.aplicacion.EjecucionIdempotente;
import co.edu.unimagdalena.samariantour.nucleo.solicitudes.aplicacion.EnviarNotificacionesService;
import co.edu.unimagdalena.samariantour.nucleo.solicitudes.aplicacion.EnviarSolicitudService;
import co.edu.unimagdalena.samariantour.nucleo.solicitudes.aplicacion.PurgarClavesIdempotenciaService;
import co.edu.unimagdalena.samariantour.nucleo.solicitudes.aplicacion.ResponderCondicionesService;
import co.edu.unimagdalena.samariantour.nucleo.solicitudes.aplicacion.VencerSolicitudesService;
import co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.puerto.entrada.AtenderSolicitudUseCase;
import co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.puerto.entrada.ConsultarColaOperadorUseCase;
import co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.puerto.entrada.ConsultarSolicitudesTuristaUseCase;
import co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.puerto.entrada.EnviarNotificacionesUseCase;
import co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.puerto.entrada.EnviarSolicitudUseCase;
import co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.puerto.entrada.PurgarClavesIdempotenciaUseCase;
import co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.puerto.entrada.ResponderCondicionesUseCase;
import co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.puerto.entrada.VencerSolicitudesUseCase;
import co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.puerto.salida.AvisoPlataformaPort;
import co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.puerto.salida.CatalogoConsultaPort;
import co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.puerto.salida.ContactoPort;
import co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.puerto.salida.IdempotenciaPort;
import co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.puerto.salida.NotificacionRepositoryPort;
import co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.puerto.salida.NotificationPort;
import co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.puerto.salida.PrestadoresPort;
import co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.puerto.salida.SolicitudRepositoryPort;
import co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.puerto.salida.TransaccionPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

/**
 * Crea los casos de uso de las solicitudes y los conecta con sus adaptadores. El de pagos está en
 * {@link SolicitudesPagosConfig}.
 */
@Configuration
public class SolicitudesConfig {

    @Bean
    EjecucionIdempotente ejecucionIdempotente(IdempotenciaPort claves, TransaccionPort transaccion,
                                              EjecutorConReintento reintento, Clock reloj) {
        return new EjecucionIdempotente(claves, transaccion, reintento, reloj);
    }

    @Bean
    EnviarSolicitudUseCase enviarSolicitud(SolicitudRepositoryPort solicitudes, CatalogoConsultaPort catalogo,
                                           PrestadoresPort prestadores, ContactoPort contactos,
                                           NotificacionRepositoryPort notificaciones,
                                           EjecucionIdempotente idempotente, Clock reloj) {
        return new EnviarSolicitudService(solicitudes, catalogo, prestadores, contactos, notificaciones, idempotente,
                reloj);
    }

    @Bean
    ConsultarSolicitudesTuristaUseCase consultarSolicitudesTurista(SolicitudRepositoryPort solicitudes) {
        return new ConsultarSolicitudesTuristaService(solicitudes);
    }

    @Bean
    ResponderCondicionesUseCase responderCondiciones(SolicitudRepositoryPort solicitudes,
                                                     NotificacionRepositoryPort notificaciones,
                                                     EjecucionIdempotente idempotente, Clock reloj) {
        return new ResponderCondicionesService(solicitudes, notificaciones, idempotente, reloj);
    }

    @Bean
    ConsultarColaOperadorUseCase consultarColaOperador(SolicitudRepositoryPort solicitudes,
                                                       PrestadoresPort prestadores, AvisoPlataformaPort avisos) {
        return new ConsultarColaOperadorService(solicitudes, prestadores, avisos);
    }

    @Bean
    AtenderSolicitudUseCase atenderSolicitud(SolicitudRepositoryPort solicitudes,
                                             NotificacionRepositoryPort notificaciones, TransaccionPort transaccion,
                                             EjecutorConReintento reintento, Clock reloj) {
        return new AtenderSolicitudService(solicitudes, notificaciones, transaccion, reintento, reloj);
    }

    @Bean
    VencerSolicitudesUseCase vencerSolicitudes(SolicitudRepositoryPort solicitudes,
                                               NotificacionRepositoryPort notificaciones, TransaccionPort transaccion,
                                               Clock reloj) {
        return new VencerSolicitudesService(solicitudes, notificaciones, transaccion, reloj);
    }

    @Bean
    EnviarNotificacionesUseCase enviarNotificaciones(NotificacionRepositoryPort notificaciones,
                                                     NotificationPort whatsapp, ContactoPort contactos,
                                                     PrestadoresPort prestadores, TransaccionPort transaccion,
                                                     Clock reloj) {
        return new EnviarNotificacionesService(notificaciones, whatsapp, contactos, prestadores, transaccion, reloj);
    }

    @Bean
    PurgarClavesIdempotenciaUseCase purgarClavesIdempotencia(IdempotenciaPort claves, TransaccionPort transaccion,
                                                             Clock reloj) {
        return new PurgarClavesIdempotenciaService(claves, transaccion, reloj);
    }
}
