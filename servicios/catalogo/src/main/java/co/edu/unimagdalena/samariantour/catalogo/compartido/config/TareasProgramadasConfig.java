package co.edu.unimagdalena.samariantour.catalogo.compartido.config;

import net.javacrumbs.shedlock.core.LockProvider;
import net.javacrumbs.shedlock.provider.mongo.MongoLockProvider;
import net.javacrumbs.shedlock.spring.annotation.EnableSchedulerLock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.mongodb.MongoDatabaseFactory;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Tareas programadas del servicio, como la limpieza de fotos sin uso (DC-47).
 *
 * <p>Con el perfil {@code mongo} las réplicas comparten los datos: cada tarea toma un candado en la colección
 * {@code shedLock} y la réplica que no lo consigue se salta esa ejecución. Con los datos en memoria cada réplica tiene
 * los suyos, así que no hace falta candado: {@code @SchedulerLock} no tiene efecto.
 */
@Configuration
@EnableScheduling
public class TareasProgramadasConfig {

    @Configuration
    @Profile("mongo")
    @EnableSchedulerLock(defaultLockAtMostFor = "PT30M")
    static class CandadoEnMongo {

        @Bean
        LockProvider candadoDeTareas(MongoDatabaseFactory baseDeDatos) {
            return new MongoLockProvider(baseDeDatos.getMongoDatabase());
        }
    }
}
