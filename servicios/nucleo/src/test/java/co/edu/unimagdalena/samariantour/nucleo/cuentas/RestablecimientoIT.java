package co.edu.unimagdalena.samariantour.nucleo.cuentas;

import co.edu.unimagdalena.samariantour.nucleo.IntegracionPostgresBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.testcontainers.containers.GenericContainer;

import java.io.IOException;
import java.net.URI;
import java.net.URLDecoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * El restablecimiento de la contraseña de punta a punta (RF-07, RF-08, DN-30): el correo sale por SMTP a un Mailpit
 * real, el mismo del piloto, y el token se lee del correo recibido.
 */
@SpringBootTest(properties = {"nucleo.bandeja.publicacion.cron=-", "nucleo.solicitudes.vencimiento.cron=-",
        "nucleo.solicitudes.notificaciones.cron=-"})
@AutoConfigureMockMvc
@ActiveProfiles("demo")
class RestablecimientoIT extends IntegracionPostgresBase {

    private static final GenericContainer<?> MAILPIT = new GenericContainer<>("axllent/mailpit:v1.21")
            .withExposedPorts(1025, 8025);
    private static final Pattern TOKEN = Pattern.compile("token=([^\\s\"\\\\]+)");
    private static final String CARLOS = "carlos.perez@demo.samariantour.co";

    static {
        MAILPIT.start();
    }

    @Autowired
    private MockMvc http;

    @Autowired
    private JdbcTemplate jdbc;

    private final HttpClient cliente = HttpClient.newHttpClient();

    @DynamicPropertySource
    static void correo(DynamicPropertyRegistry registro) {
        registro.add("spring.mail.host", MAILPIT::getHost);
        registro.add("spring.mail.port", () -> MAILPIT.getMappedPort(1025));
    }

    @BeforeEach
    void vaciarBuzon() throws Exception {
        cliente.send(HttpRequest.newBuilder(mailpit("/api/v1/messages")).DELETE().build(),
                HttpResponse.BodyHandlers.discarding());
    }

    @Test
    @DisplayName("Carlos pide el enlace, le llega al correo, pone otra contraseña y entra con ella")
    void restablecer() throws Exception {
        http.perform(post("/cuentas/contrasena/restablecimiento").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"correo\": \"%s\"}".formatted(CARLOS)))
                .andExpect(status().isAccepted());
        String correo = ultimoCorreo();
        assertThat(correo).contains("Carlos Pérez").contains("http://localhost:8080/restablecer-contrasena?token=")
                .contains("30 minutos");
        Matcher enlace = TOKEN.matcher(correo);
        assertThat(enlace.find()).isTrue();
        String token = URLDecoder.decode(enlace.group(1), StandardCharsets.UTF_8);

        nuevaContrasena(token, "Caribe2026").andExpect(status().isNoContent());
        nuevaContrasena(token, "Caribe2027")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("CODIGO_NO_VIGENTE"));
        http.perform(post("/cuentas/sesion").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"correo\": \"%s\", \"contrasena\": \"Caribe2026\"}".formatted(CARLOS)))
                .andExpect(status().isOk());
        // Le devuelve la contraseña de la demo, que comparte con Ana, para las demás pruebas.
        jdbc.update("UPDATE usuarios SET hash_contrasena = (SELECT hash_contrasena FROM usuarios "
                + "WHERE correo = 'ana.torres@demo.samariantour.co') WHERE correo = ?", CARLOS);
    }

    @Test
    @DisplayName("un correo que no está registrado responde 202 igual y no se envía nada")
    void correoDesconocido() throws Exception {
        http.perform(post("/cuentas/contrasena/restablecimiento").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"correo\": \"nadie@demo.samariantour.co\"}"))
                .andExpect(status().isAccepted());

        assertThat(get("/api/v1/messages")).contains("\"total\":0");
    }

    private ResultActions nuevaContrasena(String token, String contrasena) throws Exception {
        return http.perform(post("/cuentas/contrasena").contentType(MediaType.APPLICATION_JSON)
                .content("{\"token\": \"%s\", \"contrasena\": \"%s\"}".formatted(token, contrasena)));
    }

    /** El texto del correo más reciente, de la API de Mailpit. */
    private String ultimoCorreo() throws IOException, InterruptedException {
        return get("/api/v1/message/latest");
    }

    private String get(String ruta) throws IOException, InterruptedException {
        return cliente.send(HttpRequest.newBuilder(mailpit(ruta)).GET().build(),
                HttpResponse.BodyHandlers.ofString()).body();
    }

    private static URI mailpit(String ruta) {
        return URI.create("http://%s:%d%s".formatted(MAILPIT.getHost(), MAILPIT.getMappedPort(8025), ruta));
    }
}
