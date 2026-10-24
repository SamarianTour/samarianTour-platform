package co.edu.unimagdalena.samariantour.nucleo.administracion.infraestructura.entrada.rest;

import co.edu.unimagdalena.samariantour.nucleo.administracion.dominio.excepcion.CatalogoNoDisponibleException;
import co.edu.unimagdalena.samariantour.nucleo.administracion.dominio.excepcion.ModeracionYaDecididaException;
import co.edu.unimagdalena.samariantour.nucleo.administracion.dominio.excepcion.NoEstaEnModeracionException;
import co.edu.unimagdalena.samariantour.nucleo.administracion.dominio.excepcion.VersionNoVigenteException;
import co.edu.unimagdalena.samariantour.nucleo.administracion.dominio.modelo.Decision;
import co.edu.unimagdalena.samariantour.nucleo.administracion.dominio.modelo.DecisionModeracion;
import co.edu.unimagdalena.samariantour.nucleo.administracion.dominio.puerto.entrada.ModerarOfertaUseCase;
import co.edu.unimagdalena.samariantour.nucleo.administracion.dominio.puerto.entrada.ModerarOpinionUseCase;
import co.edu.unimagdalena.samariantour.nucleo.administracion.dominio.puerto.entrada.comando.ModerarOfertaCommand;
import co.edu.unimagdalena.samariantour.nucleo.administracion.dominio.puerto.entrada.comando.ModerarOpinionCommand;
import co.edu.unimagdalena.samariantour.nucleo.compartido.seguridad.IdentidadPorHeaders;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.time.Instant;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Las rutas de moderación del administrador (RF-79, RF-80, DN-08). */
@WebMvcTest(ModeracionAdminController.class)
@Import(IdentidadPorHeaders.class)
@ActiveProfiles("demo")
class ModeracionAdminControllerTest {

    private static final UUID ADMINISTRADOR = UUID.fromString("60000000-0000-0000-0000-000000000006");
    private static final Instant AHORA = Instant.parse("2026-10-24T15:00:00Z");
    private static final String LANCHA = "b00000000000000000000017";

    @Autowired
    private MockMvc http;

    @MockitoBean
    private ModerarOfertaUseCase moderarOferta;

    @MockitoBean
    private ModerarOpinionUseCase moderarOpinion;

    @Test
    @DisplayName("decidir una oferta entrega al caso de uso la versión, la decisión y el administrador del header")
    void decidirOferta() throws Exception {
        when(moderarOferta.moderar(any())).thenReturn(DecisionModeracion.deOferta(LANCHA, 1, Decision.RECHAZAR,
                "Sin fotos", ADMINISTRADOR, AHORA));

        http.perform(comoAdministrador(post("/admin/moderacion/ofertas/{id}/decision", LANCHA))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"numeroVersion\": 1, \"decision\": \"RECHAZAR\", \"motivo\": \"Sin fotos\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tipo").value("OFERTA"))
                .andExpect(jsonPath("$.numeroVersion").value(1))
                .andExpect(jsonPath("$.decision").value("RECHAZAR"))
                .andExpect(jsonPath("$.decididaEn").value(AHORA.toString()));

        verify(moderarOferta).moderar(new ModerarOfertaCommand(LANCHA, 1, Decision.RECHAZAR, "Sin fotos",
                ADMINISTRADOR));
    }

    @Test
    @DisplayName("decidir una opinión no lleva versión; la respuesta tampoco")
    void decidirOpinion() throws Exception {
        when(moderarOpinion.moderar(any())).thenReturn(DecisionModeracion.deOpinion("d01", Decision.APROBAR, null,
                ADMINISTRADOR, AHORA));

        http.perform(comoAdministrador(post("/admin/moderacion/opiniones/{id}/decision", "d01"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"decision\": \"APROBAR\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tipo").value("OPINION"))
                .andExpect(jsonPath("$.numeroVersion").doesNotExist())
                .andExpect(jsonPath("$.motivo").doesNotExist());

        verify(moderarOpinion).moderar(new ModerarOpinionCommand("d01", Decision.APROBAR, null, ADMINISTRADOR));
    }

    @Test
    @DisplayName("una decisión desconocida, sin versión o un JSON ilegible responden 400 sin llegar al caso de uso")
    void datosInvalidos() throws Exception {
        http.perform(comoAdministrador(post("/admin/moderacion/ofertas/{id}/decision", LANCHA))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"numeroVersion\": 1, \"decision\": \"PUBLICAR\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("DATO_INVALIDO"))
                .andExpect(jsonPath("$.detail").value("decision: debe ser APROBAR o RECHAZAR"));
        http.perform(comoAdministrador(post("/admin/moderacion/ofertas/{id}/decision", LANCHA))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"decision\": \"APROBAR\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("numeroVersion: es obligatorio y empieza en 1"));
        http.perform(comoAdministrador(post("/admin/moderacion/opiniones/{id}/decision", "d01"))
                        .contentType(MediaType.APPLICATION_JSON).content("{decision"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("DATO_INVALIDO"));

        verifyNoInteractions(moderarOferta, moderarOpinion);
    }

    @Test
    @DisplayName("traduce cada error: 404 sin moderación, 409 versión vieja o ya decidida, 503 catálogo caído")
    void errores() throws Exception {
        String cuerpo = "{\"numeroVersion\": 1, \"decision\": \"APROBAR\"}";
        doThrow(new NoEstaEnModeracionException("La oferta " + LANCHA)).when(moderarOferta).moderar(any());
        esperar(cuerpo, 404, "NO_ESTA_EN_MODERACION");

        doThrow(new VersionNoVigenteException(LANCHA, 1, 2)).when(moderarOferta).moderar(any());
        esperar(cuerpo, 409, "VERSION_NO_VIGENTE");

        doThrow(new ModeracionYaDecididaException("La versión 1",
                Decision.RECHAZAR)).when(moderarOferta).moderar(any());
        esperar(cuerpo, 409, "MODERACION_YA_DECIDIDA");

        doThrow(new CatalogoNoDisponibleException("no respondió a tiempo")).when(moderarOferta).moderar(any());
        esperar(cuerpo, 503, "CATALOGO_NO_DISPONIBLE");
    }

    @Test
    @DisplayName("solo el administrador: otro rol responde 403")
    void soloAdministrador() throws Exception {
        http.perform(post("/admin/moderacion/ofertas/{id}/decision", LANCHA)
                        .header("X-Usuario-Id", "70000000-0000-0000-0000-000000000007").header("X-Rol", "TURISTA")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"numeroVersion\": 1, \"decision\": \"APROBAR\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.codigo").value("ACCESO_DENEGADO"));
        verifyNoInteractions(moderarOferta);
    }

    private void esperar(String cuerpo, int estado, String codigo) throws Exception {
        http.perform(comoAdministrador(post("/admin/moderacion/ofertas/{id}/decision", LANCHA))
                        .contentType(MediaType.APPLICATION_JSON).content(cuerpo))
                .andExpect(status().is(estado))
                .andExpect(jsonPath("$.codigo").value(codigo));
    }

    private static MockHttpServletRequestBuilder comoAdministrador(MockHttpServletRequestBuilder peticion) {
        return peticion.header("X-Usuario-Id", ADMINISTRADOR.toString()).header("X-Rol", "ADMINISTRADOR");
    }
}
