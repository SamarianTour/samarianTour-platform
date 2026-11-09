package co.edu.unimagdalena.samariantour.nucleo.administracion;

import co.edu.unimagdalena.samariantour.nucleo.IntegracionPostgresBase;
import co.edu.unimagdalena.samariantour.seguridadjwt.TokensDePrueba;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Los pendientes de la bandeja y de pagos de punta a punta, con PostgreSQL (DN-44): el administrador reenvía un aviso
 * rechazado y devuelve a la cola un evento fallido, y la dispersión fallida de un prestador vuelve a pendiente cuando
 * él cambia su cuenta de cobro.
 */
@SpringBootTest(properties = "nucleo.bandeja.publicacion.cron=-")
@AutoConfigureMockMvc
@ActiveProfiles("demo")
class PendientesAdminIT extends IntegracionPostgresBase {

    private static final String ADMINISTRADOR = TokensDePrueba.bearer("60000000-0000-0000-0000-000000000006",
            "ADMINISTRADOR");
    /** La agencia de la demo, con su RNT y su cuenta de cobro (DN-35). */
    private static final UUID AGENCIA = UUID.fromString("10000000-0000-0000-0000-000000000001");

    @Autowired
    private MockMvc http;

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    @DisplayName("un aviso rechazado sale en la lista y se reenvía; otra vez, 409; uno que no existe, 404")
    void reenviarAvisoRechazado() throws Exception {
        String idAviso = "it-rechazado-" + UUID.randomUUID();
        jdbc.update("""
                INSERT INTO bandeja_salida (id_aviso, destino, cuerpo, estado, creado_en, proximo_intento, intentos,
                                            ultimo_error)
                VALUES (?, 'OFERTAS', CAST(? AS jsonb), 'RECHAZADO', now(), now(), 1, '400 ZONA_NO_ENCONTRADA')
                """, idAviso, "{\"idAviso\": \"" + idAviso + "\", \"tipo\": \"MODERACION\"}");

        http.perform(get("/admin/bandeja/rechazados").header(HttpHeaders.AUTHORIZATION, ADMINISTRADOR))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].idAviso", hasItem(idAviso)));
        http.perform(post("/admin/bandeja/{id}/reenvio", idAviso).header(HttpHeaders.AUTHORIZATION, ADMINISTRADOR))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("PENDIENTE"))
                .andExpect(jsonPath("$.tipo").value("MODERACION"))
                .andExpect(jsonPath("$.intentos").value(0));
        http.perform(post("/admin/bandeja/{id}/reenvio", idAviso).header(HttpHeaders.AUTHORIZATION, ADMINISTRADOR))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("AVISO_NO_RECHAZADO"));
        http.perform(post("/admin/bandeja/no-existe/reenvio").header(HttpHeaders.AUTHORIZATION, ADMINISTRADOR))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.codigo").value("AVISO_NO_ENCONTRADO"));
        http.perform(get("/admin/bandeja/rechazados")
                        .header(HttpHeaders.AUTHORIZATION, TokensDePrueba.bearer(UUID.randomUUID(), "TURISTA")))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("un evento fallido sale en la lista y el administrador lo devuelve a la cola sin fallos ni aviso")
    void reintentarEventoFallido() throws Exception {
        UUID evento = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO eventos_pasarela (id, clave_unica, transaccion_id, referencia, estado, monto_centavos,
                                              cuerpo, recibido_en, procesamiento, intentos_procesamiento,
                                              ultimo_error, siguiente_intento_en, aviso_enviado_en)
                VALUES (?, ?, 'T-IT', 'ST-IT', 'APROBADA', 30000000, '{}', now(), 'FALLIDO', 5,
                        'IllegalStateException: falló', now(), now())
                """, evento, "T-IT-" + evento);

        http.perform(get("/admin/pagos/eventos-fallidos").header(HttpHeaders.AUTHORIZATION, ADMINISTRADOR))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].id", hasItem(evento.toString())));
        http.perform(post("/admin/pagos/eventos-fallidos/{id}/reintento", evento)
                        .header(HttpHeaders.AUTHORIZATION, ADMINISTRADOR))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.monto").value(300000))
                .andExpect(jsonPath("$.intentos").value(0));

        assertThat(jdbc.queryForMap("""
                SELECT procesamiento, intentos_procesamiento, aviso_enviado_en FROM eventos_pasarela WHERE id = ?
                """, evento)).containsEntry("procesamiento", "PENDIENTE").containsEntry("intentos_procesamiento", 0)
                .containsEntry("aviso_enviado_en", null);
        http.perform(post("/admin/pagos/eventos-fallidos/{id}/reintento", evento)
                        .header(HttpHeaders.AUTHORIZATION, ADMINISTRADOR))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("EVENTO_NO_FALLIDO"));
        http.perform(post("/admin/pagos/eventos-fallidos/{id}/reintento", UUID.randomUUID())
                        .header(HttpHeaders.AUTHORIZATION, ADMINISTRADOR))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.codigo").value("EVENTO_NO_ENCONTRADO"));
    }

    @Test
    @DisplayName("la dispersión fallida del prestador vuelve a pendiente cuando él cambia su cuenta de cobro (RF-54)")
    void reactivarDispersionAlCambiarLaCuenta() throws Exception {
        UUID dispersion = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO dispersiones (id, solicitud_id, prestador_id, monto_bruto, porcentaje_comision, comision,
                                          costo_transaccional, monto_neto, estado, intentos, ultima_causa,
                                          cuenta_destino, creada_en, aviso_enviado_en)
                VALUES (?, ?, ?, 300000, 4.00, 12000, 0, 288000, 'FALLIDA', 4, 'cuenta inexistente', '******0101',
                        ?, ?)
                """, dispersion, UUID.randomUUID(), AGENCIA, Timestamp.from(Instant.now()),
                Timestamp.from(Instant.now()));

        try {
            cambiarCuenta("{\"tipo\": \"NEQUI\", \"numero\": \"3001234500\", \"titular\": \"Mar y Selva\"}");

            assertThat(jdbc.queryForMap("""
                    SELECT estado, intentos, reactivaciones, aviso_enviado_en FROM dispersiones WHERE id = ?
                    """, dispersion)).containsEntry("estado", "PENDIENTE").containsEntry("intentos", 0)
                    .containsEntry("reactivaciones", 1).containsEntry("aviso_enviado_en", null);
        } finally {
            // Las demás pruebas usan la cuenta de la demo (******0101).
            jdbc.update("DELETE FROM dispersiones WHERE id = ?", dispersion);
            cambiarCuenta("{\"tipo\": \"AHORROS\", \"entidad\": \"Bancolombia\", \"numero\": \"40112233440101\", "
                    + "\"titular\": \"Mar y Selva\"}");
        }
    }

    private void cambiarCuenta(String cuenta) throws Exception {
        http.perform(put("/prestador/cuenta-cobro")
                        .header(HttpHeaders.AUTHORIZATION, TokensDePrueba.bearerPrestador(AGENCIA, "AGENCIA_OPERADORA"))
                        .contentType(MediaType.APPLICATION_JSON).content(cuenta))
                .andExpect(status().isOk());
    }
}
