package co.edu.unimagdalena.samariantour.nucleo;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;

/** El núcleo arranca contra PostgreSQL con el esquema que crea Flyway (DN-01). */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class NucleoApplicationIT extends IntegracionPostgresBase {

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private TestRestTemplate http;

    @Test
    @DisplayName("Flyway aplica las migraciones al arrancar y la salud incluye la base")
    void arrancaConElEsquemaDeFlyway() {
        assertThat(jdbc.queryForList("SELECT version FROM flyway_schema_history WHERE success", String.class))
                .contains("1");
        assertThat(jdbc.queryForObject("SELECT to_regclass('bandeja_salida') IS NOT NULL", Boolean.class)).isTrue();

        var salud = http.getForEntity("/actuator/health", String.class);
        assertThat(salud.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(salud.getBody()).contains("\"status\":\"UP\"");
    }
}
