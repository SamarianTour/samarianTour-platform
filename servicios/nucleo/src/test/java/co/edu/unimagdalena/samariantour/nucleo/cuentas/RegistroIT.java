package co.edu.unimagdalena.samariantour.nucleo.cuentas;

import co.edu.unimagdalena.samariantour.nucleo.IntegracionPostgresBase;
import co.edu.unimagdalena.samariantour.nucleo.cuentas.dominio.puerto.salida.NotificationPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.startsWith;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * El registro de punta a punta (RF-01 a RF-03, RF-09 a RF-11, DN-18): la política, el registro, el código que llega por
 * WhatsApp y la verificación, con PostgreSQL.
 */
@SpringBootTest(properties = {"nucleo.bandeja.publicacion.cron=-", "nucleo.solicitudes.vencimiento.cron=-",
        "nucleo.solicitudes.notificaciones.cron=-"})
@AutoConfigureMockMvc
@ActiveProfiles("demo")
class RegistroIT extends IntegracionPostgresBase {

    private static final String WHATSAPP = "+573001234567";

    @Autowired
    private MockMvc http;

    @Autowired
    private JdbcTemplate jdbc;

    @MockitoSpyBean
    private NotificationPort whatsapp;

    @BeforeEach
    void vaciar() {
        String deEstasPruebas = "usuario_id IN (SELECT id FROM usuarios WHERE correo LIKE '%@registro.co')";
        jdbc.update("DELETE FROM codigos_un_uso WHERE " + deEstasPruebas);
        jdbc.update("DELETE FROM aceptaciones_politica WHERE " + deEstasPruebas);
        jdbc.update("DELETE FROM usuarios WHERE correo LIKE '%@registro.co'");
    }

    @Test
    @DisplayName("la política vigente es la v1 y su texto declara la transferencia de datos al prestador (RF-11)")
    void politica() throws Exception {
        http.perform(get("/politica-datos/vigente"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.version").value("v1"))
                .andExpect(jsonPath("$.urlTexto").value("/api/nucleo/politica-datos/v1"));
        http.perform(get("/politica-datos/v1"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("text/markdown"))
                .andExpect(content().string(containsString("transferencia de datos a un "
                        + "tercero")));
        http.perform(get("/politica-datos/v9")).andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("el turista se registra, recibe el código por WhatsApp y con él verifica su número")
    void registrarYVerificar() throws Exception {
        String id = registrar("laura@registro.co")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.rol").value("TURISTA"))
                .andExpect(jsonPath("$.whatsappVerificado").value(false))
                .andExpect(jsonPath("$.contrasena").doesNotExist())
                .andReturn().getResponse().getContentAsString().replaceAll(".*\"id\":\"([^\"]+)\".*", "$1");
        assertThat(jdbc.queryForObject("SELECT version FROM aceptaciones_politica WHERE usuario_id = ?",
                String.class, UUID.fromString(id))).isEqualTo("v1");
        String codigo = ultimoCodigo();

        verificar(id, "000000".equals(codigo) ? "111111" : "000000")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("CODIGO_INCORRECTO"))
                .andExpect(jsonPath("$.intentosRestantes").value(2));
        verificar(id, codigo)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.whatsappVerificado").value(true));
        assertThat(jdbc.queryForObject("SELECT whatsapp_verificado FROM usuarios WHERE id = ?", Boolean.class,
                UUID.fromString(id))).isTrue();
    }

    @Test
    @DisplayName("el mismo correo con otras mayúsculas responde 409 CORREO_YA_REGISTRADO; sin política, 400")
    void errores() throws Exception {
        registrar("repetido@registro.co").andExpect(status().isCreated());

        registrar("Repetido@Registro.co")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("CORREO_YA_REGISTRADO"));
        http.perform(post("/cuentas/registro").contentType(MediaType.APPLICATION_JSON).content("""
                        {"rol": "TURISTA", "nombre": "Ana", "correo": "ana@registro.co", "contrasena": "Playa2026",
                         "whatsapp": "+573001234567", "versionPolitica": "v1"}"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(startsWith("aceptaPolitica:")));
        http.perform(post("/cuentas/registro").contentType(MediaType.APPLICATION_JSON).content("""
                        {"rol": "TURISTA", "nombre": "Ana", "correo": "ana@registro.co", "contrasena": "Playa2026",
                         "whatsapp": "+573001234567", "aceptaPolitica": true, "versionPolitica": "v0"}"""))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("POLITICA_NO_VIGENTE"));
    }

    private ResultActions registrar(String correo) throws Exception {
        return http.perform(post("/cuentas/registro").contentType(MediaType.APPLICATION_JSON).content("""
                {"rol": "TURISTA", "nombre": "Laura Gómez", "correo": "%s", "contrasena": "Playa2026",
                 "whatsapp": "%s", "aceptaPolitica": true, "versionPolitica": "v1"}""".formatted(correo, WHATSAPP)));
    }

    private ResultActions verificar(String turistaId, String codigo) throws Exception {
        return http.perform(post("/turista/whatsapp/verificacion")
                .header("X-Usuario-Id", turistaId).header("X-Rol", "TURISTA")
                .contentType(MediaType.APPLICATION_JSON).content("{\"codigo\": \"%s\"}".formatted(codigo)));
    }

    private String ultimoCodigo() {
        ArgumentCaptor<String> codigo = ArgumentCaptor.forClass(String.class);
        verify(whatsapp, atLeastOnce()).enviarCodigoWhatsapp(eq(WHATSAPP), codigo.capture());
        assertThat(codigo.getValue()).matches("\\d{6}");
        return codigo.getValue();
    }

}
