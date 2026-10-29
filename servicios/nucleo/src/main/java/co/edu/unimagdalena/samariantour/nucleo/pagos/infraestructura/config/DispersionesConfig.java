package co.edu.unimagdalena.samariantour.nucleo.pagos.infraestructura.config;

import co.edu.unimagdalena.samariantour.nucleo.pagos.aplicacion.EjecutarDispersionesService;
import co.edu.unimagdalena.samariantour.nucleo.pagos.dominio.puerto.entrada.EjecutarDispersionesUseCase;
import co.edu.unimagdalena.samariantour.nucleo.pagos.dominio.puerto.salida.AvisoPagoPort;
import co.edu.unimagdalena.samariantour.nucleo.pagos.dominio.puerto.salida.ContactosPagosPort;
import co.edu.unimagdalena.samariantour.nucleo.pagos.dominio.puerto.salida.CuentasCobroPort;
import co.edu.unimagdalena.samariantour.nucleo.pagos.dominio.puerto.salida.DispersionRepositoryPort;
import co.edu.unimagdalena.samariantour.nucleo.pagos.dominio.puerto.salida.PagoTardioRepositoryPort;
import co.edu.unimagdalena.samariantour.nucleo.pagos.dominio.puerto.salida.PaymentGatewayPort;
import co.edu.unimagdalena.samariantour.nucleo.pagos.dominio.puerto.salida.TransaccionPort;
import co.edu.unimagdalena.samariantour.nucleo.pagos.infraestructura.salida.demo.PropiedadesDemoPagos;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

/**
 * Crea las dispersiones y los avisos de pagos (RF-50, RF-53, RF-54, DN-22, DN-24), y registra las cuentas de cobro y
 * los contactos provisionales de la demo [SUPUESTO] (DN-26).
 */
@Configuration
@EnableConfigurationProperties(PropiedadesDemoPagos.class)
public class DispersionesConfig {

    @Bean
    EjecutarDispersionesUseCase ejecutarDispersiones(DispersionRepositoryPort dispersiones,
                                                     PagoTardioRepositoryPort tardios, CuentasCobroPort cuentas,
                                                     ContactosPagosPort contactos, PaymentGatewayPort pasarela,
                                                     AvisoPagoPort avisos, TransaccionPort transaccion,
                                                     PropiedadesPagos propiedades, Clock reloj) {
        return new EjecutarDispersionesService(dispersiones, tardios, cuentas, contactos, pasarela, avisos,
                transaccion, propiedades.dispersion().esperaBase(), reloj);
    }
}
