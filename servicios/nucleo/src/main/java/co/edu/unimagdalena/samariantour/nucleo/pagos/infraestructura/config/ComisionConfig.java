package co.edu.unimagdalena.samariantour.nucleo.pagos.infraestructura.config;

import co.edu.unimagdalena.samariantour.nucleo.pagos.aplicacion.ActualizarComisionService;
import co.edu.unimagdalena.samariantour.nucleo.pagos.aplicacion.AdministrarEventosFallidosService;
import co.edu.unimagdalena.samariantour.nucleo.pagos.aplicacion.ConsultarDispersionesPorRecibirService;
import co.edu.unimagdalena.samariantour.nucleo.pagos.aplicacion.ConsultarPagosTardiosService;
import co.edu.unimagdalena.samariantour.nucleo.pagos.aplicacion.ReactivarDispersionesService;
import co.edu.unimagdalena.samariantour.nucleo.pagos.dominio.puerto.entrada.ActualizarComisionUseCase;
import co.edu.unimagdalena.samariantour.nucleo.pagos.dominio.puerto.entrada.AdministrarEventosFallidosUseCase;
import co.edu.unimagdalena.samariantour.nucleo.pagos.dominio.puerto.entrada.ConsultarDispersionesPorRecibirUseCase;
import co.edu.unimagdalena.samariantour.nucleo.pagos.dominio.puerto.entrada.ConsultarPagosTardiosUseCase;
import co.edu.unimagdalena.samariantour.nucleo.pagos.dominio.puerto.entrada.ReactivarDispersionesUseCase;
import co.edu.unimagdalena.samariantour.nucleo.pagos.dominio.puerto.salida.ComisionRepositoryPort;
import co.edu.unimagdalena.samariantour.nucleo.pagos.dominio.puerto.salida.DispersionRepositoryPort;
import co.edu.unimagdalena.samariantour.nucleo.pagos.dominio.puerto.salida.EventoPasarelaRepositoryPort;
import co.edu.unimagdalena.samariantour.nucleo.pagos.dominio.puerto.salida.PagoTardioRepositoryPort;
import co.edu.unimagdalena.samariantour.nucleo.pagos.dominio.puerto.salida.TransaccionPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

/**
 * Crea lo que la administración y los prestadores usan de pagos por su {@code api}: la comisión, los pagos tardíos y
 * los eventos fallidos (RF-50, RF-82, RF-83, DN-44), y la reactivación de las dispersiones al cambiar la cuenta de
 * cobro (RF-54, DN-44).
 */
@Configuration
public class ComisionConfig {

    @Bean
    ActualizarComisionUseCase actualizarComision(ComisionRepositoryPort comisiones, TransaccionPort transaccion,
                                                 Clock reloj) {
        return new ActualizarComisionService(comisiones, transaccion, reloj);
    }

    @Bean
    ConsultarPagosTardiosUseCase consultarPagosTardios(PagoTardioRepositoryPort tardios) {
        return new ConsultarPagosTardiosService(tardios);
    }

    @Bean
    AdministrarEventosFallidosUseCase administrarEventosFallidos(EventoPasarelaRepositoryPort eventos,
                                                                 TransaccionPort transaccion, Clock reloj) {
        return new AdministrarEventosFallidosService(eventos, transaccion, reloj);
    }

    @Bean
    ReactivarDispersionesUseCase reactivarDispersiones(DispersionRepositoryPort dispersiones, Clock reloj) {
        return new ReactivarDispersionesService(dispersiones, reloj);
    }

    @Bean
    ConsultarDispersionesPorRecibirUseCase dispersionesPorRecibir(DispersionRepositoryPort dispersiones) {
        return new ConsultarDispersionesPorRecibirService(dispersiones);
    }
}
