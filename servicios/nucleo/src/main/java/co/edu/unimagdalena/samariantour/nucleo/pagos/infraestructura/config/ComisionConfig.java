package co.edu.unimagdalena.samariantour.nucleo.pagos.infraestructura.config;

import co.edu.unimagdalena.samariantour.nucleo.pagos.aplicacion.ActualizarComisionService;
import co.edu.unimagdalena.samariantour.nucleo.pagos.aplicacion.ConsultarPagosTardiosService;
import co.edu.unimagdalena.samariantour.nucleo.pagos.dominio.puerto.entrada.ActualizarComisionUseCase;
import co.edu.unimagdalena.samariantour.nucleo.pagos.dominio.puerto.entrada.ConsultarPagosTardiosUseCase;
import co.edu.unimagdalena.samariantour.nucleo.pagos.dominio.puerto.salida.ComisionRepositoryPort;
import co.edu.unimagdalena.samariantour.nucleo.pagos.dominio.puerto.salida.PagoTardioRepositoryPort;
import co.edu.unimagdalena.samariantour.nucleo.pagos.dominio.puerto.salida.TransaccionPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

/** Crea lo que la administración usa por {@code pagos.api}: la comisión y los pagos tardíos (RF-50, RF-82, RF-83). */
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
}
