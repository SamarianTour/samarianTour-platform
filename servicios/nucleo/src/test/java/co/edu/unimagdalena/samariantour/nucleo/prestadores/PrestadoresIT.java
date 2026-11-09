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
import java.time.Clock;
import java.time.LocalDate;
import java.util.Base64;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

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

    @Autowired
    private Clock reloj;

    @MockitoSpyBean
    private NotificacionPrestadorPort whatsapp;

    /** Las cuentas que suprimieron sus datos: ya no tienen el correo de estas pruebas. */
    private static final Set<UUID> SUPRIMIDAS = ConcurrentHashMap.newKeySet();

    @BeforeEach
    void vaciar() {
        String deEstasPruebas = "IN (SELECT id FROM usuarios WHERE correo LIKE '%" + DOMINIO + "'"
                + SUPRIMIDAS.stream().map(id -> " OR id = '" + id + "'").collect(Collectors.joining()) + ")";
        jdbc.update("DELETE FROM bandeja_salida WHERE id_aviso LIKE 'habilitacion-%'"
                + " OR id_aviso LIKE 'supresion-prestador-%'");
        jdbc.update("DELETE FROM dispersiones WHERE prestador_id " + deEstasPruebas);
        jdbc.update("DELETE FROM solicitudes WHERE prestador_id " + deEstasPruebas);
        jdbc.update("DELETE FROM auditoria_prestadores WHERE prestador_id " + deEstasPruebas);
        jdbc.update("DELETE FROM cuentas_cobro WHERE prestador_id " + deEstasPruebas);
        jdbc.update("DELETE FROM prestadores WHERE id " + deEstasPruebas);
        jdbc.update("DELETE FROM codigos_un_uso WHERE usuario_id " + deEstasPruebas);
        jdbc.update("DELETE FROM sesiones WHERE usuario_id " + deEstasPruebas);
        jdbc.update("DELETE FROM aceptaciones_politica WHERE usuario_id " + deEstasPruebas);
        jdbc.update("DELETE FROM solicitudes_supresion WHERE usuario_id " + deEstasPruebas);
        jdbc.update("DELETE FROM usuarios WHERE id " + deEstasPruebas);
        SUPRIMIDAS.clear();
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

    @Test
    @DisplayName("la supresión pide la contraseña, se niega con compromisos y luego borra la cuenta y libera el RNT")
    void supresion() throws Exception {
        UUID hostal = cuenta("supresion");
        registrar(hostal, "910007", "ALOJAMIENTO", 6).andExpect(status().isCreated());
        suprimir(hostal, "otra")
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.codigo").value("CONTRASENA_INCORRECTA"));

        UUID dispersion = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO dispersiones (id, solicitud_id, prestador_id, monto_bruto, porcentaje_comision, comision,
                                          costo_transaccional, monto_neto, estado, intentos, cuenta_destino,
                                          creada_en)
                VALUES (?, ?, ?, 300000, 4.00, 12000, 0, 288000, 'PENDIENTE', 0, '******0707', now())
                """, dispersion, UUID.randomUUID(), hostal);
        // Hoy en Colombia, como lo cuenta el núcleo. No sirve current_date: PostgreSQL lo calcula en la zona de la
        // sesión, que el driver toma de la JVM, y en el CI es UTC, que de 19:00 a 24:00 de Colombia va un día adelante.
        LocalDate hoy = LocalDate.now(reloj);
        UUID pagada = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO solicitudes (id, turista_id, oferta_id, tipo_oferta, nombre_oferta, prestador_id, zona_id,
                                         fecha_inicio, fecha_salida, personas, operador_id, estado,
                                         intento_pago_en_curso, intentos_pago_fallidos, creada_en, actualizada_en,
                                         revision)
                VALUES (?, ?, 'OF-IT', 'ALOJAMIENTO', 'Hostal', ?, 'taganga', ?, ?, 2, ?, 'PAGADA', false, 0, now(),
                        now(), 0)
                """, pagada, UUID.randomUUID(), hostal, hoy.minusDays(1), hoy.plusDays(1), UUID.randomUUID());
        suprimir(hostal, "Minca2026")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("PRESTADOR_CON_COMPROMISOS"))
                .andExpect(jsonPath("$.compromisos.length()").value(2));

        jdbc.update("DELETE FROM dispersiones WHERE id = ?", dispersion);
        jdbc.update("UPDATE solicitudes SET fecha_inicio = ?, fecha_salida = ? WHERE id = ?", hoy.minusDays(2),
                hoy.minusDays(1), pagada);
        SUPRIMIDAS.add(hostal);
        suprimir(hostal, "Minca2026").andExpect(status().isNoContent());

        assertThat(jdbc.queryForObject("SELECT cuerpo::text FROM bandeja_salida WHERE id_aviso = ?", String.class,
                "supresion-prestador-" + hostal)).contains("\"tipo\": \"SUPRESION_PRESTADOR\"");
        assertThat(jdbc.queryForObject("SELECT cuerpo::text FROM bandeja_salida WHERE id_aviso = ?", String.class,
                "habilitacion-" + hostal + "-v2")).contains("\"rntActivo\": false");
        assertThat(jdbc.queryForMap("SELECT correo, hash_contrasena, estado FROM usuarios WHERE id = ?", hostal))
                .containsEntry("correo", "suprimido-" + hostal + "@cuentas.invalid")
                .containsEntry("hash_contrasena", null).containsEntry("estado", "SUPRIMIDA");
        assertThat(jdbc.queryForObject("SELECT numero_rnt FROM prestadores WHERE id = ?", String.class, hostal))
                .as("soporte contable").isEqualTo("910007");
        http.perform(como(get("/prestador/registro"), hostal)).andExpect(status().isNotFound());
        http.perform(post("/cuentas/sesion").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"correo\": \"supresion" + DOMINIO + "\", \"contrasena\": \"Minca2026\"}"))
                .andExpect(status().isUnauthorized());
        registrar(cuenta("sucesor"), "910007", "ALOJAMIENTO", 6).andExpect(status().isCreated());
    }

    private ResultActions suprimir(UUID prestador, String contrasena) throws Exception {
        return http.perform(como(post("/prestador/supresion"), prestador).contentType(MediaType.APPLICATION_JSON)
                .content("{\"contrasena\": \"" + contrasena + "\"}"));
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
