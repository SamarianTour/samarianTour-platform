package co.edu.unimagdalena.samariantour.catalogo;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MongoDBContainer;

/**
 * Base de las pruebas de integración: un MongoDB real en Docker, con la misma versión que el piloto.
 * El contenedor se comparte entre todas las clases de prueba (patrón singleton) y Testcontainers lo elimina al terminar.
 */
public abstract class IntegracionMongoBase {

    static final MongoDBContainer MONGO = new MongoDBContainer("mongo:7.0");

    static {
        MONGO.start();
    }

    @DynamicPropertySource
    static void configurarMongo(DynamicPropertyRegistry registro) {
        registro.add("spring.data.mongodb.uri", () -> MONGO.getReplicaSetUrl("catalogo_db"));
    }
}
