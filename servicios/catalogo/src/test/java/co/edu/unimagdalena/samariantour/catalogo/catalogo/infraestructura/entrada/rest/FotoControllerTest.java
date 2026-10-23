package co.edu.unimagdalena.samariantour.catalogo.catalogo.infraestructura.entrada.rest;

import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.excepcion.FotoNoEncontradaException;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.TamanoFoto;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.puerto.entrada.ConsultarFotoUseCase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(FotoController.class)
class FotoControllerTest {

    private static final String FOTO = "3f2504e0-4f89-11d3-9a0c-0305e82c3301";

    @Autowired
    private MockMvc http;

    @MockitoBean
    private ConsultarFotoUseCase fotos;

    @Test
    @DisplayName("entrega la foto como JPG, pública y sin identidad, con caché de un año porque nunca cambia")
    void entrega() throws Exception {
        byte[] jpg = {(byte) 0xFF, (byte) 0xD8, 1, 2};
        when(fotos.consultar(FOTO, TamanoFoto.MINIATURA)).thenReturn(jpg);

        http.perform(get("/fotos/" + FOTO + "/miniatura"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("image/jpeg"))
                .andExpect(content().bytes(jpg))
                .andExpect(header().string("Cache-Control", containsString("max-age=31536000")))
                .andExpect(header().string("Cache-Control", containsString("immutable")))
                .andExpect(header().string("X-Content-Type-Options", "nosniff"));
    }

    @Test
    @DisplayName("una foto que no existe, o un tamaño desconocido, responde 404 FOTO_NO_ENCONTRADA")
    void noEncontrada() throws Exception {
        when(fotos.consultar(FOTO, TamanoFoto.GALERIA)).thenThrow(new FotoNoEncontradaException(FOTO));

        http.perform(get("/fotos/" + FOTO + "/galeria"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.codigo").value("FOTO_NO_ENCONTRADA"));
        http.perform(get("/fotos/" + FOTO + "/original"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.codigo").value("FOTO_NO_ENCONTRADA"));
    }

    @Test
    @DisplayName("el tamaño original nunca se entrega: no existe")
    void sinOriginal() throws Exception {
        http.perform(get("/fotos/" + FOTO + "/original")).andExpect(status().isNotFound());

        verifyNoInteractions(fotos);
    }
}
