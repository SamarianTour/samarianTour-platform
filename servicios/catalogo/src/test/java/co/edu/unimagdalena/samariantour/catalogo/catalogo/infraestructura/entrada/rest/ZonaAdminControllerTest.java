package co.edu.unimagdalena.samariantour.catalogo.catalogo.infraestructura.entrada.rest;

import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.excepcion.ZonaDuplicadaException;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.excepcion.ZonaNoEncontradaException;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.Coordenada;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.DatosZona;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.Zona;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.puerto.entrada.GestionarZonasUseCase;
import co.edu.unimagdalena.samariantour.seguridadjwt.TokensDePrueba;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.util.List;

import static co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.DatosDePrueba.PRESTADOR;
import static co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.DatosDePrueba.zonaActiva;
import static co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.DatosDePrueba.zonaInactiva;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** El administrador gestiona las zonas (HU-ADM-06, DC-49). */
@WebMvcTest(ZonaAdminController.class)
@ActiveProfiles("demo")
class ZonaAdminControllerTest {

    private static final String ADMINISTRADOR = "60000000-0000-0000-0000-000000000006";
    private static final Coordenada MINCA = new Coordenada(11.14, -74.12);
    private static final String MINCA_JSON = """
            {"nombre": "Minca", "descripcion": "Sierra Nevada", "ubicacion": {"latitud": 11.14, "longitud": -74.12},
             "areaProtegida": true, "buenasPracticas": ["No dejar residuos"]}""";

    @Autowired
    private MockMvc http;

    @MockitoBean
    private GestionarZonasUseCase zonas;

    @Test
    @DisplayName("lista las zonas activas e inactivas, con su estado")
    void todas() throws Exception {
        when(zonas.listarTodas()).thenReturn(List.of(zonaActiva(), zonaInactiva()));

        http.perform(comoAdministrador(get("/admin/zonas")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].activa").value(true))
                .andExpect(jsonPath("$[1].activa").value(false));
    }

    @Test
    @DisplayName("registrar responde 201 con la zona y su ficha en Location; la marca que no se envía es false")
    void registrar() throws Exception {
        DatosZona esperados = new DatosZona("Minca", "Sierra Nevada", MINCA, true, false,
                List.of("No dejar residuos"), null);
        when(zonas.registrar(esperados)).thenReturn(Zona.registrar("a00000000000000000000099", esperados));

        http.perform(comoAdministrador(post("/admin/zonas")).contentType(MediaType.APPLICATION_JSON)
                        .content(MINCA_JSON))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", endsWith("/zonas/a00000000000000000000099")))
                .andExpect(jsonPath("$.id").value("a00000000000000000000099"))
                .andExpect(jsonPath("$.activa").value(true))
                .andExpect(jsonPath("$.altaAfluencia").value(false));
    }

    @Test
    @DisplayName("editar exige activa, para que inactivar o reactivar sea explícito")
    void editarExigeActiva() throws Exception {
        http.perform(comoAdministrador(put("/admin/zonas/z1")).contentType(MediaType.APPLICATION_JSON)
                        .content(MINCA_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("DATO_INVALIDO"))
                .andExpect(jsonPath("$.detail").value("activa: es obligatorio"));
        verifyNoInteractions(zonas);
    }

    @Test
    @DisplayName("editar con activa en false inactiva la zona")
    void inactivar() throws Exception {
        DatosZona esperados = new DatosZona("Minca", "Sierra Nevada", MINCA, true, false,
                List.of("No dejar residuos"), null);
        when(zonas.editar("z1", esperados, false))
                .thenReturn(Zona.registrar("z1", esperados).editar(esperados, false));

        http.perform(comoAdministrador(put("/admin/zonas/z1")).contentType(MediaType.APPLICATION_JSON)
                        .content(conActiva(false)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activa").value(false));
        verify(zonas).editar("z1", esperados, false);
    }

    @Test
    @DisplayName("un nombre repetido responde 409 ZONA_DUPLICADA y una zona inexistente, 404 ZONA_NO_ENCONTRADA")
    void errores() throws Exception {
        when(zonas.registrar(any())).thenThrow(new ZonaDuplicadaException("Minca"));
        when(zonas.editar(eq("no-existe"), any(), anyBoolean()))
                .thenThrow(new ZonaNoEncontradaException("no-existe"));

        http.perform(comoAdministrador(post("/admin/zonas")).contentType(MediaType.APPLICATION_JSON)
                        .content(MINCA_JSON))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("ZONA_DUPLICADA"));
        http.perform(comoAdministrador(put("/admin/zonas/no-existe")).contentType(MediaType.APPLICATION_JSON)
                        .content(conActiva(true)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.codigo").value("ZONA_NO_ENCONTRADA"));
    }

    @Test
    @DisplayName("sin descripción o con la ubicación incompleta responde 400 sin llegar al caso de uso")
    void datosInvalidos() throws Exception {
        http.perform(comoAdministrador(post("/admin/zonas")).contentType(MediaType.APPLICATION_JSON)
                        .content(MINCA_JSON.replace("\"descripcion\": \"Sierra Nevada\", ", "")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("descripcion: es obligatorio"));
        http.perform(comoAdministrador(post("/admin/zonas")).contentType(MediaType.APPLICATION_JSON)
                        .content(MINCA_JSON.replace(", \"longitud\": -74.12", "")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("DATO_INVALIDO"));
        verifyNoInteractions(zonas);
    }

    @Test
    @DisplayName("solo el administrador: sin identidad responde 401 y con otro rol, 403")
    void soloAdministrador() throws Exception {
        http.perform(post("/admin/zonas").contentType(MediaType.APPLICATION_JSON).content(MINCA_JSON))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.codigo").value("NO_AUTENTICADO"));
        http.perform(get("/admin/zonas")
                        .header(HttpHeaders.AUTHORIZATION,
                                TokensDePrueba.bearerPrestador(PRESTADOR.toString(), "AGENCIA_OPERADORA")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.codigo").value("ACCESO_DENEGADO"));
        verifyNoInteractions(zonas);
    }

    private static String conActiva(boolean activa) {
        return MINCA_JSON.replace("]}", "], \"activa\": %s}".formatted(activa));
    }

    private static MockHttpServletRequestBuilder comoAdministrador(MockHttpServletRequestBuilder peticion) {
        return peticion.header(HttpHeaders.AUTHORIZATION, TokensDePrueba.bearer(ADMINISTRADOR, "ADMINISTRADOR"));
    }
}
