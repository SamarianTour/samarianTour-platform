package co.edu.unimagdalena.samariantour.nucleo.cuentas;

import co.edu.unimagdalena.samariantour.nucleo.IntegracionPostgresBase;
import co.edu.unimagdalena.samariantour.nucleo.compartido.cifrado.CifradorDatos;
import co.edu.unimagdalena.samariantour.nucleo.cuentas.dominio.modelo.Usuario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Los datos personales (RF-12) y su supresión (RF-13, DN-31) de punta a punta, con PostgreSQL: qué queda en las tablas
 * y el aviso que espera el catálogo en la bandeja.
 */
@SpringBootTest(properties = {"nucleo.bandeja.publicacion.cron=-", "nucleo.solicitudes.vencimiento.cron=-",
        "nucleo.solicitudes.notificaciones.cron=-"})
@AutoConfigureMockMvc
@ActiveProfiles("demo")
class SupresionIT extends IntegracionPostgresBase {

    private static final String CORREO = "titular@supresion.co";

    @Autowired
    private MockMvc http;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private CifradorDatos cifrador;

    @BeforeEach
    void vaciar() {
        String deEstasPruebas = "usuario_id IN (SELECT id FROM usuarios WHERE correo LIKE '%@supresion.co')";
        for (String tabla : new String[] {"sesiones", "codigos_un_uso", "aceptaciones_politica",
                "solicitudes_supresion"}) {
            jdbc.update("DELETE FROM " + tabla + " WHERE " + deEstasPruebas);
        }
        jdbc.update("DELETE FROM usuarios WHERE correo LIKE '%@supresion.co'");
    }

    @Test
    @DisplayName("cualquier rol consulta y cambia su nombre en /cuenta; el prestador no manda WhatsApp")
    void datosPersonales() throws Exception {
        UUID turista = registrar();
        http.perform(como(get("/cuenta"), turista, "TURISTA"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.correo").value(CORREO));
        http.perform(como(put("/cuenta"), turista, "TURISTA").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\": \"Titular Nuevo\", \"whatsapp\": \"+573001234567\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Titular Nuevo"));

        UUID operador = UUID.fromString("60000000-0000-0000-0000-000000000001");
        http.perform(como(get("/cuenta"), operador, "OPERADOR_DISPONIBILIDAD"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rol").value("OPERADOR_DISPONIBILIDAD"))
                .andExpect(jsonPath("$.whatsapp").doesNotExist());
        http.perform(como(put("/cuenta"), operador, "OPERADOR_DISPONIBILIDAD").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\": \"Operador 1\", \"whatsapp\": \"+573001234567\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(startsWith("whatsapp:")));
        http.perform(get("/cuenta")).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("la supresión anonimiza la fila, conserva la constancia y deja en la bandeja SUPRESION_TURISTA")
    void suprimir() throws Exception {
        UUID turista = registrar();
        http.perform(como(post("/turista/supresion"), turista, "TURISTA").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"contrasena\": \"otra\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.codigo").value("CONTRASENA_INCORRECTA"));

        http.perform(como(post("/turista/supresion"), turista, "TURISTA").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"contrasena\": \"Playa2026\"}"))
                .andExpect(status().isNoContent());

        Map<String, Object> fila = jdbc.queryForMap("SELECT * FROM usuarios WHERE id = ?", turista);
        assertThat(fila).containsEntry("estado", "SUPRIMIDA").containsEntry("correo",
                "suprimido-" + turista + "@cuentas.invalid").containsEntry("hash_contrasena", null)
                .containsEntry("whatsapp_cifrado", null);
        assertThat(cifrador.descifrar((String) fila.get("nombre_cifrado"))).isEqualTo(Usuario.NOMBRE_SUPRIMIDO);
        assertThat(jdbc.queryForObject("SELECT estado FROM solicitudes_supresion WHERE usuario_id = ?", String.class,
                turista)).isEqualTo("EJECUTADA");
        assertThat(jdbc.queryForObject("SELECT count(*) FROM aceptaciones_politica WHERE usuario_id = ?",
                Integer.class, turista)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT cuerpo::text FROM bandeja_salida WHERE id_aviso = ?", String.class,
                "supresion-" + turista)).contains("\"tipo\": \"SUPRESION_TURISTA\"")
                .contains("\"turistaId\": \"" + turista + "\"");

        http.perform(post("/cuentas/sesion").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"correo\": \"%s\", \"contrasena\": \"Playa2026\"}".formatted(CORREO)))
                .andExpect(status().isUnauthorized());
        http.perform(como(get("/cuenta"), turista, "TURISTA")).andExpect(status().isNotFound());
        registrar();
    }

    private UUID registrar() throws Exception {
        String respuesta = http.perform(post("/cuentas/registro").contentType(MediaType.APPLICATION_JSON).content("""
                        {"rol": "TURISTA", "nombre": "Titular", "correo": "%s", "contrasena": "Playa2026",
                         "whatsapp": "+573001234567", "aceptaPolitica": true, "versionPolitica": "v1"}"""
                        .formatted(CORREO)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return UUID.fromString(respuesta.replaceAll(".*\"id\":\"([^\"]+)\".*", "$1"));
    }

    private static MockHttpServletRequestBuilder como(MockHttpServletRequestBuilder peticion, UUID usuario,
                                                      String rol) {
        return peticion.header("X-Usuario-Id", usuario.toString()).header("X-Rol", rol);
    }
}
