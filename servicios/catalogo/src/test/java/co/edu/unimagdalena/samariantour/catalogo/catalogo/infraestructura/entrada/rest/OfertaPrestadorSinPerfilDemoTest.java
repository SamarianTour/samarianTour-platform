package co.edu.unimagdalena.samariantour.catalogo.catalogo.infraestructura.entrada.rest;

import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.puerto.entrada.GestionarOfertaUseCase;
import co.edu.unimagdalena.samariantour.catalogo.compartido.seguridad.IdentidadNoDisponible;
import co.edu.unimagdalena.samariantour.catalogo.compartido.seguridad.IdentidadPorHeaders;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.DatosDePrueba.PRESTADOR;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Sin el perfil demo, los headers que sustituyen al token no se aceptan (DC-01). */
@WebMvcTest(OfertaPrestadorController.class)
@Import({IdentidadPorHeaders.class, IdentidadNoDisponible.class})
class OfertaPrestadorSinPerfilDemoTest {

    @Autowired
    private MockMvc http;

    @MockitoBean
    private GestionarOfertaUseCase gestionarOferta;

    @Test
    @DisplayName("sin el perfil demo, publicar con los headers de identidad responde 401 NO_AUTENTICADO")
    void headersNoAceptados() throws Exception {
        http.perform(post("/prestador/ofertas").contentType(MediaType.APPLICATION_JSON).content("{}")
                        .header("X-Usuario-Id", PRESTADOR.toString()).header("X-Rol", "PRESTADOR")
                        .header("X-Tipo-Prestador", "AGENCIA_OPERADORA"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.codigo").value("NO_AUTENTICADO"));

        verifyNoInteractions(gestionarOferta);
    }
}
