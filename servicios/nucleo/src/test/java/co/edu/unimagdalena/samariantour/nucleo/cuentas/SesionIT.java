package co.edu.unimagdalena.samariantour.nucleo.cuentas;

import co.edu.unimagdalena.samariantour.nucleo.IntegracionPostgresBase;
import co.edu.unimagdalena.samariantour.nucleo.cuentas.infraestructura.salida.token.JwtRs256Adapter;
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
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** La sesión de punta a punta (RF-04 a RF-06, DN-19), con las cuentas de la demo y PostgreSQL. */
@SpringBootTest(properties = {"nucleo.bandeja.publicacion.cron=-", "nucleo.solicitudes.vencimiento.cron=-",
        "nucleo.solicitudes.notificaciones.cron=-"})
@AutoConfigureMockMvc
@ActiveProfiles("demo")
class SesionIT extends IntegracionPostgresBase {

    private static final String LAURA = "70000000-0000-0000-0000-000000000007";
    private static final String ANDRES = "80000000-0000-0000-0000-000000000008";

    @Autowired
    private MockMvc http;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private JwtRs256Adapter tokens;

    @BeforeEach
    void desbloquear() {
        jdbc.update("UPDATE usuarios SET intentos_fallidos = 0, bloqueada_hasta = NULL WHERE id IN (?::uuid, ?::uuid)",
                LAURA, ANDRES);
    }

    @Test
    @DisplayName("Laura inicia sesión con la clave de la demo y el JWT dice quién es y su rol, firmado por el núcleo")
    void iniciar() throws Exception {
        String respuesta = iniciar("laura@demo.samariantour.co", "Demo1234")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tipoToken").value("Bearer"))
                .andExpect(jsonPath("$.cuenta.id").value(LAURA))
                .andExpect(jsonPath("$.cuenta.whatsappVerificado").value(true))
                .andReturn().getResponse().getContentAsString();

        Jwt jwt = NimbusJwtDecoder.withPublicKey(tokens.clavePublica()).build().decode(campo(respuesta, "tokenAcceso"));
        assertThat(jwt.getSubject()).isEqualTo(LAURA);
        assertThat(jwt.getClaimAsString("rol")).isEqualTo("TURISTA");
        assertThat(jwt.getClaimAsString("sid")).isEqualTo(campo(respuesta, "tokenRenovacion").split("\\.")[0]);
    }

    @Test
    @DisplayName("renovar rota el token; el viejo cierra la sesión; cerrar invalida el token de renovación")
    void renovarYCerrar() throws Exception {
        String primero = campo(iniciar("laura@demo.samariantour.co", "Demo1234").andReturn().getResponse()
                .getContentAsString(), "tokenRenovacion");

        String segundo = campo(conToken("/cuentas/sesion/renovacion", primero)
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(), "tokenRenovacion");
        conToken("/cuentas/sesion/renovacion", primero)
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.codigo").value("SESION_INVALIDA"));
        conToken("/cuentas/sesion/renovacion", segundo).andExpect(status().isUnauthorized());

        String otro = campo(iniciar("laura@demo.samariantour.co", "Demo1234").andReturn().getResponse()
                .getContentAsString(), "tokenRenovacion");
        conToken("/cuentas/sesion/cierre", otro).andExpect(status().isNoContent());
        conToken("/cuentas/sesion/renovacion", otro).andExpect(status().isUnauthorized());
        conToken("/cuentas/sesion/cierre", "cualquier-cosa").andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("cinco fallos bloquean 15 minutos aunque lleguen a la vez: el sexto ya responde 423 (RF-05)")
    void bloqueoConIntentosSimultaneos() throws Exception {
        Callable<Integer> fallo = () -> iniciar("andres@demo.samariantour.co", "Equivocada1")
                .andReturn().getResponse().getStatus();
        List<Future<Integer>> enParalelo;
        try (var hilos = Executors.newFixedThreadPool(5)) {
            enParalelo = hilos.invokeAll(List.of(fallo, fallo, fallo, fallo, fallo));
        }
        List<Integer> estados = enParalelo.stream().map(SesionIT::estado).sorted().toList();

        assertThat(estados).containsExactly(401, 401, 401, 401, 423);
        iniciar("andres@demo.samariantour.co", "Demo1234")
                .andExpect(status().isLocked())
                .andExpect(jsonPath("$.codigo").value("CUENTA_BLOQUEADA"))
                .andExpect(jsonPath("$.bloqueadaHasta").exists());
        iniciar("nadie@demo.samariantour.co", "Demo1234")
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.codigo").value("CREDENCIALES_INVALIDAS"));
    }

    private ResultActions iniciar(String correo, String contrasena) throws Exception {
        return http.perform(post("/cuentas/sesion").contentType(MediaType.APPLICATION_JSON)
                .content("{\"correo\": \"%s\", \"contrasena\": \"%s\"}".formatted(correo, contrasena)));
    }

    private ResultActions conToken(String ruta, String tokenRenovacion) throws Exception {
        return http.perform(post(ruta).contentType(MediaType.APPLICATION_JSON)
                .content("{\"tokenRenovacion\": \"%s\"}".formatted(tokenRenovacion)));
    }

    private static String campo(String json, String nombre) {
        return json.replaceAll(".*\"" + nombre + "\":\"([^\"]+)\".*", "$1");
    }

    private static int estado(Future<Integer> resultado) {
        try {
            return resultado.get();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
