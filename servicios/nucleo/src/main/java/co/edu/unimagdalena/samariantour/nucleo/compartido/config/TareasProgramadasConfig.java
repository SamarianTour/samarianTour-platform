package co.edu.unimagdalena.samariantour.nucleo.compartido.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Tareas programadas del servicio, como la publicación de la bandeja de salida (DN-06). No usan un candado entre
 * réplicas: cada tarea reparte el trabajo en la base, como la bandeja con {@code FOR UPDATE SKIP LOCKED}.
 */
@Configuration
@EnableScheduling
public class TareasProgramadasConfig {
}
