package co.edu.unimagdalena.samariantour.nucleo.solicitudes.infraestructura.config;

import co.edu.unimagdalena.samariantour.nucleo.solicitudes.aplicacion.RegistrarEventoPagoService;
import co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.puerto.entrada.RegistrarEventoPagoUseCase;
import co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.puerto.salida.AvisosCatalogoPort;
import co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.puerto.salida.NotificacionRepositoryPort;
import co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.puerto.salida.SolicitudRepositoryPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

/** Crea el caso de uso que el módulo de pagos usa por {@code solicitudes.api} (DN-17). */
@Configuration
public class SolicitudesPagosConfig {

    @Bean
    RegistrarEventoPagoUseCase registrarEventoPago(SolicitudRepositoryPort solicitudes,
                                                   NotificacionRepositoryPort notificaciones,
                                                   AvisosCatalogoPort catalogo, Clock reloj) {
        return new RegistrarEventoPagoService(solicitudes, notificaciones, catalogo, reloj);
    }
}
