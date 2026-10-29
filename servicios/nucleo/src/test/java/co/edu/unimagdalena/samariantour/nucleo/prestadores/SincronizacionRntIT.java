package co.edu.unimagdalena.samariantour.nucleo.prestadores;

import co.edu.unimagdalena.samariantour.nucleo.IntegracionPostgresBase;
import co.edu.unimagdalena.samariantour.nucleo.prestadores.dominio.puerto.entrada.SincronizarRntUseCase;
import co.edu.unimagdalena.samariantour.nucleo.prestadores.dominio.puerto.salida.NotificacionPrestadorPort;
import co.edu.unimagdalena.samariantour.nucleo.prestadores.infraestructura.salida.rnt.RntSimuladoAdapter;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * La sincronización con el RNT de punta a punta (RF-76 a RF-78, DN-36), con los prestadores de la demo, el RNT
 * simulado y PostgreSQL: el retiro y la restitución dejan su aviso al catálogo en la bandeja.
 */
@SpringBootTest(properties = "nucleo.bandeja.publicacion.cron=-")
@AutoConfigureMockMvc
@ActiveProfiles("demo")
class SincronizacionRntIT extends IntegracionPostgresBase {

    private static final String AGENCIA = "10000000-0000-0000-0000-000000000001";

    @Autowired
    private MockMvc http;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private RntSimuladoAdapter rnt;

    @Autowired
    private SincronizarRntUseCase sincronizacion;

    @MockitoSpyBean
    private NotificacionPrestadorPort whatsapp;

    /** Los de {@code PrestadoresPersistenciaIT} están activos sin estar en la tabla simulada: los retiraría. */
    @BeforeEach
    void vaciar() {
        jdbc.update("DELETE FROM prestadores WHERE numero_rnt LIKE '99%'");
    }

    /** Las demás pruebas cuentan con la agencia de la demo activa. */
    @AfterEach
    void restituir() {
        if (rnt.restituir("910001")) {
            sincronizacion.sincronizar();
        }
        jdbc.update("DELETE FROM bandeja_salida WHERE id_aviso LIKE 'habilitacion-%'");
    }

    @Test
    @DisplayName("el RNT de la agencia se da de baja: queda Inactiva, sale del catálogo y se le avisa; luego vuelve")
    void retiroYRestitucion() throws Exception {
        long version = jdbc.queryForObject("SELECT version_habilitacion FROM prestadores WHERE numero_rnt = '910001'",
                Long.class);

        http.perform(comoAdministrador(delete("/admin/rnt-simulado/910001")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.retirados").value(1))
                .andExpect(jsonPath("$.restituidos").value(0))
                .andExpect(jsonPath("$.interrumpida").value(false));

        http.perform(get("/prestador/registro").header("X-Usuario-Id", AGENCIA)
                        .header("X-Rol", "PRESTADOR").header("X-Tipo-Prestador", "AGENCIA_OPERADORA"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estadoRnt").value("INACTIVO"))
                .andExpect(jsonPath("$.habilitado").value(false));
        assertThat(aviso(version + 1)).contains("\"rntActivo\": false");
        verify(whatsapp).avisarRetiroCatalogo("+573001110101", "910001");

        http.perform(comoAdministrador(put("/admin/rnt-simulado/910001")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.retirados").value(0))
                .andExpect(jsonPath("$.restituidos").value(1));
        assertThat(aviso(version + 2)).contains("\"rntActivo\": true");
    }

    @Test
    @DisplayName("el administrador la ejecuta cuando quiera; sin cambios en el RNT, nadie sale ni vuelve")
    void bajoDemanda() throws Exception {
        http.perform(comoAdministrador(post("/admin/rnt/sincronizacion")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.revisados").value(greaterThanOrEqualTo(5)))
                .andExpect(jsonPath("$.retirados").value(0))
                .andExpect(jsonPath("$.restituidos").value(0))
                .andExpect(jsonPath("$.sinRespuesta").value(0));
        verify(whatsapp, never()).avisarRetiroCatalogo(anyString(), anyString());

        http.perform(post("/admin/rnt/sincronizacion").header("X-Usuario-Id", AGENCIA).header("X-Rol", "PRESTADOR")
                        .header("X-Tipo-Prestador", "AGENCIA_OPERADORA"))
                .andExpect(status().isForbidden());
        http.perform(comoAdministrador(delete("/admin/rnt-simulado/999999")))
                .andExpect(status().isNotFound());
    }

    private String aviso(long version) {
        return jdbc.queryForObject("SELECT cuerpo::text FROM bandeja_salida WHERE id_aviso = ?", String.class,
                "habilitacion-" + AGENCIA + "-v" + version);
    }

    private static MockHttpServletRequestBuilder comoAdministrador(MockHttpServletRequestBuilder peticion) {
        return peticion.header("X-Usuario-Id", "60000000-0000-0000-0000-000000000006")
                .header("X-Rol", "ADMINISTRADOR");
    }
}
