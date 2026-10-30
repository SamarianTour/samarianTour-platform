package co.edu.unimagdalena.samariantour.catalogo.catalogo.infraestructura.entrada.rest;

import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.excepcion.DatoInvalidoException;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.excepcion.PerfilNoEncontradoException;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.Foto;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.PerfilPublico;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.puerto.entrada.GestionarPerfilPublicoUseCase;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.puerto.entrada.comando.GuardarPerfilCommand;
import co.edu.unimagdalena.samariantour.seguridadjwt.TokensDePrueba;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.util.List;

import static co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.DatosDePrueba.PRESTADOR;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PerfilPrestadorController.class)
@ActiveProfiles("demo")
class PerfilPrestadorControllerTest {

    private static final Foto FOTO = new Foto("3f2504e0-4f89-11d3-9a0c-0305e82c3301");

    @Autowired
    private MockMvc http;

    @MockitoBean
    private GestionarPerfilPublicoUseCase perfiles;

    @Test
    @DisplayName("el prestador ve su perfil con la dirección de cada tamaño de sus fotos")
    void consultar() throws Exception {
        when(perfiles.consultar(PRESTADOR)).thenReturn(perfilCon(List.of(FOTO)));

        http.perform(comoPrestador(get("/prestador/perfil")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombreComercial").value("Aves de Minca"))
                .andExpect(jsonPath("$.descripcion").value("Guías de aves"))
                .andExpect(jsonPath("$.fotos", hasSize(1)))
                .andExpect(jsonPath("$.fotos[0].id").value(FOTO.id()))
                .andExpect(jsonPath("$.fotos[0].url").value("/fotos/" + FOTO.id() + "/galeria"))
                .andExpect(jsonPath("$.fotos[0].urlMiniatura").value("/fotos/" + FOTO.id() + "/miniatura"));
    }

    @Test
    @DisplayName("sin perfil todavía responde 404 PERFIL_NO_ENCONTRADO")
    void sinPerfil() throws Exception {
        when(perfiles.consultar(PRESTADOR)).thenThrow(new PerfilNoEncontradoException(PRESTADOR));

        http.perform(comoPrestador(get("/prestador/perfil")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.codigo").value("PERFIL_NO_ENCONTRADO"));
    }

    @Test
    @DisplayName("guarda los datos del perfil del prestador de la identidad, nunca de otro")
    void guardar() throws Exception {
        when(perfiles.guardar(any())).thenReturn(perfilCon(List.of()));

        http.perform(comoPrestador(put("/prestador/perfil")).contentType(MediaType.APPLICATION_JSON).content("""
                        {"nombreComercial": "Aves de Minca", "descripcion": "Guías de aves",
                         "horariosAtencion": "Todos los días", "sitioWeb": "https://ejemplo.co",
                         "redesSociales": ["https://ejemplo.co/red"]}
                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.descripcion").value("Guías de aves"));

        ArgumentCaptor<GuardarPerfilCommand> comando = ArgumentCaptor.forClass(GuardarPerfilCommand.class);
        verify(perfiles).guardar(comando.capture());
        assertThat(comando.getValue()).isEqualTo(new GuardarPerfilCommand(PRESTADOR, "Aves de Minca",
                "Guías de aves", "Todos los días", "https://ejemplo.co", List.of("https://ejemplo.co/red")));
    }

    @Test
    @DisplayName("sube una foto como multipart y responde 201 con el perfil")
    void agregarFoto() throws Exception {
        byte[] imagen = {1, 2, 3};
        when(perfiles.agregarFoto(eq(PRESTADOR), any())).thenReturn(perfilCon(List.of(FOTO)));

        http.perform(comoPrestador(multipart("/prestador/perfil/fotos")
                        .file(new MockMultipartFile("foto", "playa.jpg", "image/jpeg", imagen))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.fotos[0].id").value(FOTO.id()));

        verify(perfiles).agregarFoto(PRESTADOR, imagen);
    }

    @Test
    @DisplayName("una subida sin la parte foto, vacía o rechazada responde 400 DATO_INVALIDO")
    void subidaInvalida() throws Exception {
        http.perform(comoPrestador(multipart("/prestador/perfil/fotos")
                        .file(new MockMultipartFile("archivo", "playa.jpg", "image/jpeg", new byte[] {1}))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("DATO_INVALIDO"));
        http.perform(comoPrestador(multipart("/prestador/perfil/fotos")
                        .file(new MockMultipartFile("foto", "vacia.jpg", "image/jpeg", new byte[0]))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("DATO_INVALIDO"));

        when(perfiles.agregarFoto(eq(PRESTADOR), any()))
                .thenThrow(new DatoInvalidoException("foto", "debe ser una imagen JPG, PNG o WebP"));
        http.perform(comoPrestador(multipart("/prestador/perfil/fotos")
                        .file(new MockMultipartFile("foto", "virus.jpg", "image/jpeg", new byte[] {1}))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("DATO_INVALIDO"));
    }

    @Test
    @DisplayName("quitar una foto responde 204")
    void quitarFoto() throws Exception {
        http.perform(comoPrestador(delete("/prestador/perfil/fotos/" + FOTO.id())))
                .andExpect(status().isNoContent());

        verify(perfiles).quitarFoto(PRESTADOR, FOTO.id());
    }

    @Test
    @DisplayName("el perfil es solo del prestador: sin identidad 401 y con otro rol 403 (DC-13)")
    void soloPrestador() throws Exception {
        http.perform(get("/prestador/perfil"))
                .andExpect(status().isUnauthorized());
        http.perform(get("/prestador/perfil")
                        .header(HttpHeaders.AUTHORIZATION, TokensDePrueba.bearer(PRESTADOR.toString(), "TURISTA")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.codigo").value("ACCESO_DENEGADO"));

        verifyNoInteractions(perfiles);
    }

    private static PerfilPublico perfilCon(List<Foto> fotos) {
        return new PerfilPublico(PRESTADOR, "Aves de Minca", "Guías de aves", fotos, null, null, List.of());
    }

    private static MockHttpServletRequestBuilder comoPrestador(MockHttpServletRequestBuilder peticion) {
        return peticion
                .header(HttpHeaders.AUTHORIZATION,
                        TokensDePrueba.bearerPrestador(PRESTADOR.toString(), "GUIA_INDEPENDIENTE"));
    }
}
