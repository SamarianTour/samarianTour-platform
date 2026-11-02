package co.edu.unimagdalena.samariantour.nucleo.indicadores;

import co.edu.unimagdalena.samariantour.nucleo.IntegracionPostgresBase;
import co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.modelo.CamposDelTipo;
import co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.modelo.CategoriaPrestador;
import co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.modelo.SolicitudDisponibilidad;
import co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.puerto.salida.SolicitudRepositoryPort;
import co.edu.unimagdalena.samariantour.seguridadjwt.TokensDePrueba;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.UUID;

import static co.edu.unimagdalena.samariantour.nucleo.solicitudes.SolicitudesDePrueba.OPERADOR;
import static co.edu.unimagdalena.samariantour.nucleo.solicitudes.SolicitudesDePrueba.SIETE;
import static co.edu.unimagdalena.samariantour.nucleo.solicitudes.SolicitudesDePrueba.TAYRONA;
import static co.edu.unimagdalena.samariantour.nucleo.solicitudes.SolicitudesDePrueba.cabana;
import static co.edu.unimagdalena.samariantour.nucleo.solicitudes.SolicitudesDePrueba.caminata;
import static co.edu.unimagdalena.samariantour.nucleo.solicitudes.SolicitudesDePrueba.dosNoches;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Los indicadores de punta a punta (RF-85 a RF-89, DN-41): las solicitudes en PostgreSQL, los conteos por la API de las
 * solicitudes y las rutas del gestor, con los conteos pequeños ocultos (RET-03).
 */
@SpringBootTest(properties = "nucleo.bandeja.publicacion.cron=-")
@AutoConfigureMockMvc
@ActiveProfiles("demo")
class IndicadoresIT extends IntegracionPostgresBase {

    private static final UUID GESTOR = UUID.fromString("60000000-0000-0000-0000-000000000003");

    @Autowired
    private MockMvc http;

    @Autowired
    private SolicitudRepositoryPort repositorio;

    @Autowired
    private JdbcTemplate jdbc;

    @BeforeEach
    void preparar() {
        jdbc.update("DELETE FROM notificaciones_solicitud");
        jdbc.update("DELETE FROM transiciones_solicitud");
        jdbc.update("DELETE FROM solicitudes");
        Instant ahora = Instant.now();
        LocalDate fecha = LocalDate.now(ZoneId.of("America/Bogota")).plusDays(10);
        for (int i = 0; i < 4; i++) {
            guardar(SolicitudDisponibilidad.crear(UUID.randomUUID(), UUID.randomUUID(), caminata(), fecha,
                    CamposDelTipo.actividad(SIETE), 2, null, OPERADOR, CategoriaPrestador.MICRO_PEQUENO, ahora));
        }
        guardar(SolicitudDisponibilidad.crear(UUID.randomUUID(), UUID.randomUUID(), cabana(), fecha,
                dosNoches(fecha), 2, null, OPERADOR, CategoriaPrestador.MEDIANO, ahora));
    }

    @Test
    @DisplayName("el tablero del último mes cuenta las cuatro caminatas y oculta la cabaña, que es una sola")
    void tablero() throws Exception {
        http.perform(comoGestor(get("/gestor/indicadores/tablero")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(5))
                .andExpect(jsonPath("$.filas[0].zonaId").value(TAYRONA))
                .andExpect(jsonPath("$.filas[0].solicitudes").value(4))
                .andExpect(jsonPath("$.filas[1].tipoOferta").value("ALOJAMIENTO"))
                .andExpect(jsonPath("$.filas[1].solicitudes").doesNotExist());
    }

    @Test
    @DisplayName("el embudo y las categorías salen de las mismas solicitudes; un periodo anterior está vacío")
    void embudoYCategorias() throws Exception {
        http.perform(comoGestor(get("/gestor/indicadores/embudo")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.etapas[0].solicitudes").value(5))
                .andExpect(jsonPath("$.etapas[1].solicitudes").value(0))
                .andExpect(jsonPath("$.porTipo[0].etapas[0].solicitudes").value(4));
        http.perform(comoGestor(get("/gestor/indicadores/categorias")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.categorias[1].categoria").value("MICRO_PEQUENO"))
                .andExpect(jsonPath("$.categorias[1].porcentaje").value(80.0))
                .andExpect(jsonPath("$.categorias[2].solicitudes").doesNotExist());
        http.perform(comoGestor(get("/gestor/indicadores/tablero").param("desde", "2026-01-01")
                        .param("hasta", "2026-01-31")))
                .andExpect(jsonPath("$.total").value(0))
                .andExpect(jsonPath("$.filas").isEmpty());
    }

    @Test
    @DisplayName("solo el gestor del destino: el turista y el administrador reciben 403")
    void soloElGestor() throws Exception {
        for (String rol : new String[] {"TURISTA", "ADMINISTRADOR"}) {
            http.perform(get("/gestor/indicadores/embudo")
                            .header(HttpHeaders.AUTHORIZATION, TokensDePrueba.bearer(UUID.randomUUID(), rol)))
                    .andExpect(status().isForbidden());
        }
    }

    private void guardar(SolicitudDisponibilidad solicitud) {
        solicitud.extraerNotificaciones();
        repositorio.insertar(solicitud);
    }

    private static MockHttpServletRequestBuilder comoGestor(MockHttpServletRequestBuilder peticion) {
        return peticion.header(HttpHeaders.AUTHORIZATION, TokensDePrueba.bearer(GESTOR, "GESTOR_DESTINO"));
    }
}
