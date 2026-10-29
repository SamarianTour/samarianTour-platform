package co.edu.unimagdalena.samariantour.nucleo.pagos.infraestructura.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Registra las propiedades de pagos (DN-18). La pasarela la elige {@code nucleo.pagos.pasarela}: cada adaptador se
 * registra solo con su valor. Los casos de uso los crean {@link CobrosConfig}, {@link EventosPagoConfig},
 * {@link DispersionesConfig} y {@link ComisionConfig}.
 */
@Configuration
@EnableConfigurationProperties(PropiedadesPagos.class)
public class PagosConfig {
}
