package co.edu.unimagdalena.samariantour.nucleo.pagos.infraestructura.config;

import co.edu.unimagdalena.samariantour.nucleo.compartido.concurrencia.EjecutorConReintento;
import co.edu.unimagdalena.samariantour.nucleo.pagos.aplicacion.EjecucionIdempotente;
import co.edu.unimagdalena.samariantour.nucleo.pagos.aplicacion.GenerarSesionCobroService;
import co.edu.unimagdalena.samariantour.nucleo.pagos.aplicacion.PurgarClavesIdempotenciaService;
import co.edu.unimagdalena.samariantour.nucleo.pagos.dominio.puerto.entrada.GenerarSesionCobroUseCase;
import co.edu.unimagdalena.samariantour.nucleo.pagos.dominio.puerto.entrada.PurgarClavesIdempotenciaUseCase;
import co.edu.unimagdalena.samariantour.nucleo.pagos.dominio.puerto.salida.IdempotenciaPort;
import co.edu.unimagdalena.samariantour.nucleo.pagos.dominio.puerto.salida.IntentoPagoRepositoryPort;
import co.edu.unimagdalena.samariantour.nucleo.pagos.dominio.puerto.salida.PaymentGatewayPort;
import co.edu.unimagdalena.samariantour.nucleo.pagos.dominio.puerto.salida.SolicitudesPort;
import co.edu.unimagdalena.samariantour.nucleo.pagos.dominio.puerto.salida.TransaccionPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

/** Crea la sesión de cobro con sus claves de idempotencia (RF-46 a RF-49, DN-19, DN-20). */
@Configuration
public class CobrosConfig {

    @Bean
    EjecucionIdempotente ejecucionIdempotentePagos(IdempotenciaPort claves, TransaccionPort transaccion,
                                                   EjecutorConReintento reintento, Clock reloj) {
        return new EjecucionIdempotente(claves, transaccion, reintento, reloj);
    }

    @Bean
    GenerarSesionCobroUseCase generarSesionCobro(SolicitudesPort solicitudes, IntentoPagoRepositoryPort intentos,
                                                 PaymentGatewayPort pasarela,
                                                 EjecucionIdempotente ejecucionIdempotentePagos, Clock reloj) {
        return new GenerarSesionCobroService(solicitudes, intentos, pasarela, ejecucionIdempotentePagos, reloj);
    }

    @Bean
    PurgarClavesIdempotenciaUseCase purgarClavesIdempotenciaPagos(IdempotenciaPort claves,
                                                                  TransaccionPort transaccion, Clock reloj) {
        return new PurgarClavesIdempotenciaService(claves, transaccion, reloj);
    }
}
