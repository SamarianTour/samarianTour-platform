package co.edu.unimagdalena.samariantour.nucleo.pagos.infraestructura.config;

import co.edu.unimagdalena.samariantour.nucleo.compartido.concurrencia.EjecutorConReintento;
import co.edu.unimagdalena.samariantour.nucleo.pagos.aplicacion.ProcesarEventoPagoService;
import co.edu.unimagdalena.samariantour.nucleo.pagos.aplicacion.RecibirEventoPasarelaService;
import co.edu.unimagdalena.samariantour.nucleo.pagos.dominio.modelo.CostoTransaccional;
import co.edu.unimagdalena.samariantour.nucleo.pagos.dominio.puerto.entrada.ProcesarEventoPagoUseCase;
import co.edu.unimagdalena.samariantour.nucleo.pagos.dominio.puerto.entrada.RecibirEventoPasarelaUseCase;
import co.edu.unimagdalena.samariantour.nucleo.pagos.dominio.puerto.salida.ComisionRepositoryPort;
import co.edu.unimagdalena.samariantour.nucleo.pagos.dominio.puerto.salida.DispersionRepositoryPort;
import co.edu.unimagdalena.samariantour.nucleo.pagos.dominio.puerto.salida.EventoPasarelaRepositoryPort;
import co.edu.unimagdalena.samariantour.nucleo.pagos.dominio.puerto.salida.IntentoPagoRepositoryPort;
import co.edu.unimagdalena.samariantour.nucleo.pagos.dominio.puerto.salida.PagoTardioRepositoryPort;
import co.edu.unimagdalena.samariantour.nucleo.pagos.dominio.puerto.salida.PaymentGatewayPort;
import co.edu.unimagdalena.samariantour.nucleo.pagos.dominio.puerto.salida.SolicitudesPort;
import co.edu.unimagdalena.samariantour.nucleo.pagos.dominio.puerto.salida.TransaccionPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

/** Crea la recepción y el procesamiento de los eventos de la pasarela (RF-50 a RF-53, RF-58, DN-21, DN-22). */
@Configuration
public class EventosPagoConfig {

    @Bean
    CostoTransaccional costoTransaccional(PropiedadesPagos propiedades) {
        PropiedadesPagos.Costo costo = propiedades.costoTransaccional();
        return new CostoTransaccional(costo.porcentaje(), costo.fijo(), costo.iva());
    }

    @Bean
    RecibirEventoPasarelaUseCase recibirEventoPasarela(PaymentGatewayPort pasarela,
                                                       EventoPasarelaRepositoryPort eventos,
                                                       TransaccionPort transaccion, Clock reloj) {
        return new RecibirEventoPasarelaService(pasarela, eventos, transaccion, reloj);
    }

    @Bean
    ProcesarEventoPagoUseCase procesarEventoPago(EventoPasarelaRepositoryPort eventos,
                                                 IntentoPagoRepositoryPort intentos, SolicitudesPort solicitudes,
                                                 PagoTardioRepositoryPort tardios,
                                                 DispersionRepositoryPort dispersiones,
                                                 ComisionRepositoryPort comisiones, CostoTransaccional costo,
                                                 TransaccionPort transaccion, EjecutorConReintento reintento,
                                                 Clock reloj) {
        return new ProcesarEventoPagoService(eventos, intentos, solicitudes, tardios, dispersiones, comisiones, costo,
                transaccion, reintento, reloj);
    }
}
