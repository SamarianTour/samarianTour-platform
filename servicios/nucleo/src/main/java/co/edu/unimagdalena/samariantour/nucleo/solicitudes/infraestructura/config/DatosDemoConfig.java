package co.edu.unimagdalena.samariantour.nucleo.solicitudes.infraestructura.config;

import co.edu.unimagdalena.samariantour.nucleo.solicitudes.infraestructura.salida.demo.PropiedadesDemo;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/** Registra la tabla provisional de prestadores y contactos de la demo [SUPUESTO] (DN-15). */
@Configuration
@EnableConfigurationProperties(PropiedadesDemo.class)
public class DatosDemoConfig {
}
