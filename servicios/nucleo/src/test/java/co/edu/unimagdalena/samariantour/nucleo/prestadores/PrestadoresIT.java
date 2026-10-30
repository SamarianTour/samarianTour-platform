package co.edu.unimagdalena.samariantour.nucleo.prestadores;

import co.edu.unimagdalena.samariantour.nucleo.IntegracionPostgresBase;
import co.edu.unimagdalena.samariantour.nucleo.prestadores.dominio.puerto.salida.NotificacionPrestadorPort;
import co.edu.unimagdalena.samariantour.seguridadjwt.TokensDePrueba;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * El registro del prestador de punta a punta (RF-62, RF-63, RF-68 a RF-70, RF-73, DN-33), con el RNT simulado de la
 * demo y PostgreSQL: la cuenta se crea en el módulo de cuentas y el aviso al catálogo queda en la bandeja.
 */
@SpringBootTest(properties = "nucleo.bandeja.publicacion.cron=-")
@AutoConfigureMockMvc
@ActiveProfiles("demo")
class PrestadoresIT extends IntegracionPostgresBase {

    private static final String DOMINIO = "@prestadores-it.co";

    @Autowired
    private MockMvc http;

    @Autowired
    private JdbcTemplate jdbc;

    @MockitoSpyBean
    private NotificacionPrestadorPort whatsapp;

    @BeforeEach
    void vaciar() {
        String deEstasPruebas = "IN (SELECT id FROM usuarios WHERE correo LIKE '%" + DOMINIO + "')";
        jdbc.update("DELETE FROM bandeja_salida WHERE id_aviso LIKE 'habilitacion-%'");
        jdbc.update("DELETE FROM auditoria_prestadores WHERE prestador_id " + deEstasPruebas);
        jdbc.update("DELETE FROM cuentas_cobro WHERE prestador_id " + deEstasPruebas);
        jdbc.update("DELETE FROM prestadores WHERE id " + deEstasPruebas);
        jdbc.update("DELETE FROM codigos_un_uso WHERE usuario_id " + deEstasPruebas);
        jdbc.update("DELETE FROM sesiones WHERE usuario_id " + deEstasPruebas);
        jdbc.update("DELETE FROM aceptaciones_politica WHERE usuario_id " + deEstasPruebas);
        jdbc.update("DELETE FROM usuarios WHERE correo LIKE '%" + DOMINIO + "'");
    }

    @Test
    @DisplayName("la guía nueva registra su RNT: queda activa con los datos del RNT y el catálogo recibe HABILITACION")
    void registrar() throws Exception {
        UUID guia = cuenta("guia");

        http.perform(como(post("/prestador/registro"), guia).contentType(MediaType.APPLICATION_JSON).content("""
                        {"numeroRnt": "910006", "tipo": "GUIA_INDEPENDIENTE", "modalidades": ["CAMINATA", "KAYAK"],
                         "idiomas": ["es", "en"]}"""))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.estadoRnt").value("ACTIVO"))
                .andExpect(jsonPath("$.razonSocial").value("MARIA FERNANDA OSPINO DIAZ"))
                .andExpect(jsonPath("$.categoria").value("GUIA_INDEPENDIENTE"))
                .andExpect(jsonPath("$.habilitado").value(true));

        assertThat(jdbc.queryForObject("SELECT cuerpo::text FROM bandeja_salida WHERE id_aviso = ?", String.class,
                "habilitacion-" + guia + "-v1")).contains("\"tipo\": \"HABILITACION\"")
                .contains("\"rntActivo\": true").contains("\"version\": 1");
        http.perform(como(get("/prestador/registro"), guia))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ultimaSincronizacion").exists());
    }

    @Test
    @DisplayName("con su sesión real: el token no trae tipo hasta registrar el RNT; al renovar lo trae (DN-37)")
    void tipoEnElToken() throws Exception {
        UUID guia = cuenta("token");
        String sesion = http.perform(post("/cuentas/sesion").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"correo\": \"token" + DOMINIO + "\", \"contrasena\": \"Minca2026\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String acceso = campo(sesion, "tokenAcceso");
        assertThat(reclamos(acceso)).contains("\"rol\":\"PRESTADOR\"").doesNotContain("tipoPrestador");

        http.perform(post("/prestador/registro").header(HttpHeaders.AUTHORIZATION, "Bearer " + acceso)
                        .contentType(MediaType.APPLICATION_JSON).content("""
                        {"numeroRnt": "910006", "tipo": "GUIA_INDEPENDIENTE", "modalidades": ["CAMINATA"],
                         "idiomas": ["es"]}"""))
                .andExpect(status().isCreated());
        String renovada = http.perform(post("/cuentas/sesion/renovacion").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tokenRenovacion\": \"" + campo(sesion, "tokenRenovacion") + "\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertThat(reclamos(campo(renovada, "tokenAcceso"))).contains("\"sub\":\"" + guia + "\"")
                .contains("\"tipoPrestador\":\"GUIA_INDEPENDIENTE\"");
    }

    @Test
    @DisplayName("tipo que no coincide, RNT repetido o sin cuenta: 409, 409 y 404; un RNT que no existe, NO_ENCONTRADO")
    void rechazos() throws Exception {
        UUID hostal = cuenta("hostal");
        registrar(hostal, "910007", "RESTAURANTE", 6)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("TIPO_NO_COINCIDE_CON_RNT"));
        registrar(hostal, "910007", "ALOJAMIENTO", 6).andExpect(status().isCreated());
        registrar(cuenta("otro"), "910007", "ALOJAMIENTO", 6)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("RNT_YA_REGISTRADO"));
        registrar(UUID.randomUUID(), "910099", "ALOJAMIENTO", 6)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.codigo").value("CUENTA_NO_ENCONTRADA"));
        registrar(cuenta("sin-rnt"), "910099", "ALOJAMIENTO", 6)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.estadoRnt").value("NO_ENCONTRADO"))
                .andExpect(jsonPath("$.habilitado").value(false));

        http.perform(como(put("/prestador/tipo"), hostal).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tipo\": \"ALOJAMIENTO\", \"numeroEmpleados\": 300}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.categoria").value("GRANDE"));
        http.perform(como(put("/prestador/tipo"), hostal).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tipo\": \"HOTEL\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("DATO_INVALIDO"));
    }

    @Test
    @DisplayName("el prestador configura su WhatsApp, recibe el código y con él lo verifica (RF-71)")
    void whatsapp() throws Exception {
        UUID guia = cuenta("whatsapp");
        http.perform(como(post("/prestador/registro"), guia).contentType(MediaType.APPLICATION_JSON).content("""
                        {"numeroRnt": "910006", "tipo": "GUIA_INDEPENDIENTE", "modalidades": ["CAMINATA"],
                         "idiomas": ["es"]}"""))
                .andExpect(status().isCreated());

        http.perform(como(put("/prestador/whatsapp"), guia).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"whatsapp\": \"+573001110606\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.whatsappVerificado").value(false));
        ArgumentCaptor<String> codigo = ArgumentCaptor.forClass(String.class);
        verify(whatsapp).enviarCodigo(eq("+573001110606"), codigo.capture());
        String otro = "000000".equals(codigo.getValue()) ? "111111" : "000000";

        http.perform(como(post("/prestador/whatsapp/verificacion"), guia).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"codigo\": \"%s\"}".formatted(otro)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.intentosRestantes").value(2));
        http.perform(como(post("/prestador/whatsapp/verificacion"), guia).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"codigo\": \"%s\"}".formatted(codigo.getValue())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.whatsappVerificado").value(true));
    }

    @Test
    @DisplayName("la cuenta de cobro se guarda cifrada, sale enmascarada y al cambiarla queda en el historial (RF-74)")
    void cuentaCobro() throws Exception {
        UUID hostal = cuenta("cobro");
        registrar(hostal, "910007", "ALOJAMIENTO", 6).andExpect(status().isCreated());
        http.perform(como(get("/prestador/cuenta-cobro"), hostal))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.codigo").value("CUENTA_COBRO_NO_REGISTRADA"));

        http.perform(como(put("/prestador/cuenta-cobro"), hostal).contentType(MediaType.APPLICATION_JSON).content("""
                        {"tipo": "AHORROS", "entidad": "Bancolombia", "numero": "40112233440707",
                         "titular": "Hostal Bahia de Taganga"}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.numeroEnmascarado").value("******0707"))
                .andExpect(jsonPath("$.numero").doesNotExist());
        http.perform(como(put("/prestador/cuenta-cobro"), hostal).contentType(MediaType.APPLICATION_JSON).content("""
                        {"tipo": "NEQUI", "numero": "3001110707", "titular": "Hostal Bahia de Taganga"}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.entidad").value("Nequi"));

        assertThat(jdbc.queryForList("SELECT numero_cifrado FROM cuentas_cobro WHERE prestador_id = ?",
                String.class, hostal)).hasSize(2).allMatch(cifrado -> cifrado.startsWith("v1:"))
                .noneMatch(cifrado -> cifrado.contains("0707"));
        assertThat(jdbc.queryForObject("SELECT count(*) FROM cuentas_cobro WHERE prestador_id = ? "
                + "AND vigente_hasta IS NULL", Integer.class, hostal)).isEqualTo(1);
        assertThat(jdbc.queryForList("SELECT accion FROM auditoria_prestadores WHERE prestador_id = ? "
                + "ORDER BY ocurrido_en", String.class, hostal))
                .containsExactly("CUENTA_COBRO_REGISTRADA", "CUENTA_COBRO_CAMBIADA");
    }

    private ResultActions registrar(UUID id, String rnt, String tipo, int empleados) throws Exception {
        return http.perform(como(post("/prestador/registro"), id).contentType(MediaType.APPLICATION_JSON)
                .content("{\"numeroRnt\": \"%s\", \"tipo\": \"%s\", \"numeroEmpleados\": %d}"
                        .formatted(rnt, tipo, empleados)));
    }

    /** Una cuenta de prestador nueva, registrada en el módulo de cuentas. */
    private UUID cuenta(String nombre) throws Exception {
        String respuesta = http.perform(post("/cuentas/registro").contentType(MediaType.APPLICATION_JSON).content("""
                        {"rol": "PRESTADOR", "nombre": "Prestador %s", "correo": "%s%s", "contrasena": "Minca2026",
                         "aceptaPolitica": true, "versionPolitica": "v1"}""".formatted(nombre, nombre, DOMINIO)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return UUID.fromString(respuesta.replaceAll(".*\"id\":\"([^\"]+)\".*", "$1"));
    }

    private static String campo(String json, String nombre) {
        return json.replaceAll(".*\"" + nombre + "\":\"([^\"]+)\".*", "$1");
    }

    /** La parte central del JWT, en claro: los reclamos que firmó el núcleo. */
    private static String reclamos(String jwt) {
        return new String(Base64.getUrlDecoder().decode(jwt.split("\\.")[1]), StandardCharsets.UTF_8);
    }

    private static MockHttpServletRequestBuilder como(MockHttpServletRequestBuilder peticion, UUID prestador) {
        return peticion.header(HttpHeaders.AUTHORIZATION, TokensDePrueba.bearerPrestador(prestador, null));
    }
}
