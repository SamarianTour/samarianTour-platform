package co.edu.unimagdalena.samariantour.catalogo.catalogo.infraestructura.entrada.rest;

import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.excepcion.DatoInvalidoException;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.puerto.entrada.SubirFotoUseCase;
import co.edu.unimagdalena.samariantour.seguridadjwt.TokensDePrueba;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import static co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.DatosDePrueba.FOTO;
import static co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.DatosDePrueba.PRESTADOR;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(FotoPrestadorController.class)
@ActiveProfiles("demo")
class FotoPrestadorControllerTest {

    @Autowired
    private MockMvc http;

    @MockitoBean
    private SubirFotoUseCase fotos;

    @Test
    @DisplayName("sube la foto del prestador de la identidad y responde 201 con la dirección de cada tamaño (DC-47)")
    void subir() throws Exception {
        when(fotos.subir(eq(PRESTADOR), any())).thenReturn(FOTO);

        http.perform(comoPrestador(multipart("/prestador/fotos")
                        .file(new MockMultipartFile("foto", "playa.jpg", "image/jpeg", new byte[] {1, 2, 3}))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(FOTO.id()))
                .andExpect(jsonPath("$.url").value("/fotos/" + FOTO.id() + "/galeria"))
                .andExpect(jsonPath("$.urlMiniatura").value("/fotos/" + FOTO.id() + "/miniatura"));
    }

    @Test
    @DisplayName("sin la parte foto o con un archivo vacío responde 400 sin llamar al caso de uso")
    void sinFoto() throws Exception {
        http.perform(comoPrestador(multipart("/prestador/fotos")
                        .file(new MockMultipartFile("archivo", "playa.jpg", "image/jpeg", new byte[] {1}))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("DATO_INVALIDO"));
        http.perform(comoPrestador(multipart("/prestador/fotos")
                        .file(new MockMultipartFile("foto", "vacia.jpg", "image/jpeg", new byte[0]))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("foto: no llegó ninguna imagen"));
        verifyNoInteractions(fotos);
    }

    @Test
    @DisplayName("un archivo que no es una imagen responde 400 DATO_INVALIDO")
    void noEsImagen() throws Exception {
        when(fotos.subir(eq(PRESTADOR), any()))
                .thenThrow(new DatoInvalidoException("foto", "debe ser una imagen JPG, PNG o WebP"));

        http.perform(comoPrestador(multipart("/prestador/fotos")
                        .file(new MockMultipartFile("foto", "virus.jpg", "image/jpeg", new byte[] {1}))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("DATO_INVALIDO"));
    }

    @Test
    @DisplayName("solo un prestador sube fotos: sin identidad 401 y con otro rol 403 (DC-13)")
    void soloPrestadores() throws Exception {
        MockMultipartFile imagen = new MockMultipartFile("foto", "playa.jpg", "image/jpeg", new byte[] {1});

        http.perform(multipart("/prestador/fotos").file(imagen))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.codigo").value("NO_AUTENTICADO"));
        http.perform(multipart("/prestador/fotos").file(imagen)
                        .header(HttpHeaders.AUTHORIZATION, TokensDePrueba.bearer(PRESTADOR.toString(), "TURISTA")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.codigo").value("ACCESO_DENEGADO"));
        verifyNoInteractions(fotos);
    }

    private static MockHttpServletRequestBuilder comoPrestador(MockHttpServletRequestBuilder peticion) {
        return peticion
                .header(HttpHeaders.AUTHORIZATION,
                        TokensDePrueba.bearerPrestador(PRESTADOR.toString(), "AGENCIA_OPERADORA"));
    }
}
