package co.edu.unimagdalena.samariantour.nucleo;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;

import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

/**
 * Base de las pruebas de integración: un PostgreSQL real en Docker, con la misma versión que el piloto. El contenedor
 * se comparte entre todas las clases de prueba (patrón singleton) y Testcontainers lo elimina al terminar.
 */
public abstract class IntegracionPostgresBase {

    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:17")
            .withDatabaseName("nucleo_db")
            // Cada contexto de Spring que las pruebas guardan en caché abre su propio pool de conexiones.
            .withCommand("postgres", "-c", "max_connections=300");

    static {
        POSTGRES.start();
    }

    /** Clave de cifrado solo para las pruebas (DN-28): 32 bytes en Base64. */
    public static final String CLAVE_CIFRADO = "MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=";

    /** Clave RSA del token solo para las pruebas (DN-29), generada al cargar la clase: PKCS#8 en Base64. */
    public static final String CLAVE_TOKEN = claveRsaNueva();

    @DynamicPropertySource
    static void configurarPostgres(DynamicPropertyRegistry registro) {
        registro.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registro.add("spring.datasource.username", POSTGRES::getUsername);
        registro.add("spring.datasource.password", POSTGRES::getPassword);
        registro.add("nucleo.cifrado.clave", () -> CLAVE_CIFRADO);
        registro.add("nucleo.jwt.clave-privada", () -> CLAVE_TOKEN);
        // Spring guarda en caché el contexto de cada clase de prueba, con sus tareas programadas vivas: un vencimiento
        // o un envío de otra clase podría tomar las solicitudes de la que corre. Las pruebas llaman a esos casos de uso
        // directamente.
        registro.add("nucleo.solicitudes.vencimiento.cron", () -> "-");
        registro.add("nucleo.solicitudes.notificaciones.cron", () -> "-");
        registro.add("nucleo.administracion.sincronizacion-rnt.cron", () -> "-");
    }

    private static String claveRsaNueva() {
        try {
            KeyPairGenerator generador = KeyPairGenerator.getInstance("RSA");
            generador.initialize(2048);
            return Base64.getEncoder().encodeToString(generador.generateKeyPair().getPrivate().getEncoded());
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
