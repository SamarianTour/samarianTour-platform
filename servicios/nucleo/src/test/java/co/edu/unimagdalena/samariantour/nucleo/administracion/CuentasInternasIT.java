package co.edu.unimagdalena.samariantour.nucleo.administracion;

import co.edu.unimagdalena.samariantour.nucleo.IntegracionPostgresBase;
import co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.puerto.salida.PrestadoresPort;
import co.edu.unimagdalena.samariantour.seguridadjwt.TokensDePrueba;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.testcontainers.containers.GenericContainer;

import java.io.IOException;
import java.net.URI;
import java.net.URLDecoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Las cuentas internas y el operador de cada prestador de punta a punta (RF-32, RF-81, DN-39, DN-40): la invitación
 * sale por SMTP a un Mailpit real, como en {@code RestablecimientoIT}, y la asignación llega hasta lo que solicitudes
 * usa para repartir las solicitudes nuevas.
 */
@SpringBootTest(properties = "nucleo.bandeja.publicacion.cron=-")
@AutoConfigureMockMvc
@ActiveProfiles("demo")
class CuentasInternasIT extends IntegracionPostgresBase {

    private static final GenericContainer<?> MAILPIT = new GenericContainer<>("axllent/mailpit:v1.21")
            .withExposedPorts(1025, 8025);
    private static final Pattern TOKEN = Pattern.compile("token=([^\\s\"\\\\]+)");
    private static final UUID ADMINISTRADOR = UUID.fromString("60000000-0000-0000-0000-000000000006");
    private static final UUID OPERADOR_2 = UUID.fromString("60000000-0000-0000-0000-000000000002");
    private static final UUID SIN_OPERADOR = UUID.fromString("50000000-0000-0000-0000-000000000005");
    private static final String VALENTINA = "valentina.rios@demo.samariantour.co";

    static {
        MAILPIT.start();
    }

    @Autowired
    private MockMvc http;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private PrestadoresPort solicitudes;

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

    /** Deja la demo como estaba: la agencia sin RNT vuelve a no tener operador y la cuenta nueva se borra. */
    @AfterEach
    void restaurarDemo() {
        jdbc.update("UPDATE prestadores SET operador_id = NULL WHERE id = ?", SIN_OPERADOR);
        jdbc.update("DELETE FROM sesiones WHERE usuario_id IN (SELECT id FROM usuarios WHERE correo = ?)", VALENTINA);
        jdbc.update("DELETE FROM codigos_un_uso WHERE usuario_id IN (SELECT id FROM usuarios WHERE correo = ?)",
                VALENTINA);
        jdbc.update("DELETE FROM usuarios WHERE correo = ?", VALENTINA);
    }

    @Test
    @DisplayName("Valentina recibe la invitación, entra, atiende a la agencia sin operador y solo sale reasignándola")
    void operadorNuevo() throws Exception {
        String creada = http.perform(comoAdministrador(post("/admin/cuentas-internas"))
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {"rol": "OPERADOR_DISPONIBILIDAD", "nombre": "Valentina Ríos",
                                 "correo": "%s"}""".formatted(VALENTINA)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.contrasenaEstablecida").value(false))
                .andReturn().getResponse().getContentAsString();
        UUID valentina = UUID.fromString(creada.replaceAll(".*\"id\":\"([^\"]+)\".*", "$1"));

        String correo = ultimoCorreo();
        assertThat(correo).contains("Valentina Ríos").contains("72 horas");
        Matcher enlace = TOKEN.matcher(correo);
        assertThat(enlace.find()).isTrue();
        http.perform(post("/cuentas/contrasena").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\": \"%s\", \"contrasena\": \"Caribe2026\"}"
                                .formatted(URLDecoder.decode(enlace.group(1), StandardCharsets.UTF_8))))
                .andExpect(status().isNoContent());
        iniciarSesion().andExpect(status().isOk());

        http.perform(comoAdministrador(get("/admin/prestadores").param("sinOperador", "true")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].id", hasItem(SIN_OPERADOR.toString())));
        asignar(SIN_OPERADOR, valentina)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.operadorId").value(valentina.toString()));
        assertThat(solicitudes.operadorAsignado(SIN_OPERADOR)).contains(valentina);
        http.perform(comoAdministrador(get("/admin/prestadores").param("sinOperador", "true")))
                .andExpect(jsonPath("$[*].id", not(hasItem(SIN_OPERADOR.toString()))));

        desactivar(valentina)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("OPERADOR_CON_PRESTADORES"));

        asignar(SIN_OPERADOR, OPERADOR_2).andExpect(status().isOk());
        desactivar(valentina)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("DESACTIVADA"));
        iniciarSesion()
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.codigo").value("CUENTA_DESACTIVADA"));
        asignar(SIN_OPERADOR, valentina)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("OPERADOR_NO_VALIDO"));
    }

    @Test
    @DisplayName("el administrador no se desactiva a sí mismo, y un correo repetido no crea otra cuenta")
    void reglasDelAdministrador() throws Exception {
        desactivar(ADMINISTRADOR)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("CUENTA_PROPIA"));

        http.perform(comoAdministrador(post("/admin/cuentas-internas")).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"rol": "GESTOR_DESTINO", "nombre": "Otro gestor",
                                 "correo": "Gestor@demo.samariantour.co"}"""))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("CORREO_YA_REGISTRADO"));
        assertThat(buzon("/api/v1/messages")).contains("\"total\":0");
    }

    private ResultActions asignar(UUID prestadorId, UUID operadorId) throws Exception {
        return http.perform(comoAdministrador(put("/admin/prestadores/{id}/operador", prestadorId))
                .contentType(MediaType.APPLICATION_JSON).content("{\"operadorId\": \"%s\"}".formatted(operadorId)));
    }

    private ResultActions desactivar(UUID usuarioId) throws Exception {
        return http.perform(comoAdministrador(post("/admin/cuentas-internas/{id}/desactivacion", usuarioId)));
    }

    private ResultActions iniciarSesion() throws Exception {
        return http.perform(post("/cuentas/sesion").contentType(MediaType.APPLICATION_JSON)
                .content("{\"correo\": \"%s\", \"contrasena\": \"Caribe2026\"}".formatted(VALENTINA)));
    }

    private static MockHttpServletRequestBuilder comoAdministrador(MockHttpServletRequestBuilder peticion) {
        return peticion.header(HttpHeaders.AUTHORIZATION, TokensDePrueba.bearer(ADMINISTRADOR, "ADMINISTRADOR"));
    }

    private String ultimoCorreo() throws IOException, InterruptedException {
        return buzon("/api/v1/message/latest");
    }

    /** Lee la API de Mailpit. */
    private String buzon(String ruta) throws IOException, InterruptedException {

        return cliente.send(HttpRequest.newBuilder(mailpit(ruta)).GET().build(),
                HttpResponse.BodyHandlers.ofString()).body();
    }

    private static URI mailpit(String ruta) {
        return URI.create("http://%s:%d%s".formatted(MAILPIT.getHost(), MAILPIT.getMappedPort(8025), ruta));
    }
}
