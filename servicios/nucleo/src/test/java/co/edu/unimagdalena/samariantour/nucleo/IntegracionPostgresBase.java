package co.edu.unimagdalena.samariantour.nucleo;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * Base de las pruebas de integración: un PostgreSQL real en Docker, con la misma versión que el piloto. El contenedor
 * se comparte entre todas las clases de prueba (patrón singleton) y Testcontainers lo elimina al terminar.
 */
public abstract class IntegracionPostgresBase {

    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:17")
            .withDatabaseName("nucleo_db");

    static {
        POSTGRES.start();
    }

    @DynamicPropertySource
    static void configurarPostgres(DynamicPropertyRegistry registro) {
        registro.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registro.add("spring.datasource.username", POSTGRES::getUsername);
        registro.add("spring.datasource.password", POSTGRES::getPassword);
    }
}
