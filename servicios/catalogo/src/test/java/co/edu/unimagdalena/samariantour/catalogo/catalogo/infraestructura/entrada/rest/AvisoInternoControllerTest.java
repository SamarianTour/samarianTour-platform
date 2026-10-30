package co.edu.unimagdalena.samariantour.catalogo.catalogo.infraestructura.entrada.rest;

import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.puerto.entrada.AplicarAvisoNucleoUseCase;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.puerto.entrada.comando.AvisoHabilitacion;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.puerto.entrada.comando.AvisoModeracion;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.puerto.entrada.comando.AvisoNucleoCommand;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.puerto.entrada.comando.DecisionModeracion;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.puerto.entrada.comando.ResultadoAviso;
import co.edu.unimagdalena.samariantour.seguridadjwt.ServiciosInternos;
import co.edu.unimagdalena.samariantour.seguridadjwt.TokensDePrueba;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import static co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.DatosDePrueba.PRESTADOR;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AvisoInternoController.class)
class AvisoInternoControllerTest {

    /** El token con el que el núcleo llama a las rutas internas (DN-38). */
    private static final String COMO_NUCLEO =
            TokensDePrueba.bearerServicio(ServiciosInternos.NUCLEO, ServiciosInternos.CATALOGO);

    @Autowired
    private MockMvc http;

    @MockitoBean
    private AplicarAvisoNucleoUseCase aplicarAviso;

    @Test
    @DisplayName("un aviso de moderación llega al caso de uso con la versión y la decisión, y responde el resultado")
    void moderacion() throws Exception {
        when(aplicarAviso.aplicar(any())).thenReturn(ResultadoAviso.APLICADO);

        enviar("""
                {"idAviso": "aviso-1", "tipo": "MODERACION", "ofertaId": "oferta-1", "numeroVersion": 2,
                 "decision": "RECHAZAR", "motivo": "Falta el punto de encuentro"}
                """)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultado").value("APLICADO"));

        assertThat(avisoRecibido()).isEqualTo(new AvisoModeracion("aviso-1", "oferta-1", 2,
                DecisionModeracion.RECHAZAR, "Falta el punto de encuentro"));
    }

    @Test
    @DisplayName("un aviso de habilitación llega al caso de uso con el prestador, el RNT y la versión")
    void habilitacion() throws Exception {
        when(aplicarAviso.aplicar(any())).thenReturn(ResultadoAviso.IGNORADO);

        enviar("""
                {"idAviso": "aviso-2", "tipo": "HABILITACION", "prestadorId": "%s", "rntActivo": false, "version": 3}
                """.formatted(PRESTADOR))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultado").value("IGNORADO"));

        assertThat(avisoRecibido()).isEqualTo(new AvisoHabilitacion("aviso-2", PRESTADOR, false, 3));
    }

    @Test
    @DisplayName("un aviso repetido también responde 200, para que el núcleo no lo reintente (DC-04)")
    void duplicado() throws Exception {
        when(aplicarAviso.aplicar(any())).thenReturn(ResultadoAviso.DUPLICADO);

        enviar("""
                {"idAviso": "aviso-1", "tipo": "MODERACION", "ofertaId": "oferta-1", "numeroVersion": 1,
                 "decision": "APROBAR"}
                """)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultado").value("DUPLICADO"));
    }

    @Test
    @DisplayName("un aviso incompleto o mal formado responde 400 DATO_INVALIDO y no se aplica")
    void avisoInvalido() throws Exception {
        enviar("""
                {"idAviso": "aviso-1", "tipo": "OTRO"}
                """)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("DATO_INVALIDO"))
                .andExpect(jsonPath("$.detail", containsString("MODERACION")));
        enviar("""
                {"idAviso": "aviso-1", "tipo": "MODERACION", "ofertaId": "oferta-1", "numeroVersion": 1,
                 "decision": "RECHAZAR"}
                """)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail", containsString("motivo")));
        enviar("""
                {"idAviso": "aviso-1", "tipo": "MODERACION", "ofertaId": "oferta-1", "decision": "APROBAR"}
                """)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail", containsString("numeroVersion")));
        enviar("""
                {"idAviso": "aviso-1", "tipo": "HABILITACION", "prestadorId": "%s", "rntActivo": true, "version": 0}
                """.formatted(PRESTADOR))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail", containsString("version")));
        enviar("""
                {"idAviso": "aviso-1", "tipo": "HABILITACION", "prestadorId": "abc", "rntActivo": true, "version": 1}
                """)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail", containsString("prestadorId")));

        verifyNoInteractions(aplicarAviso);
    }

    private ResultActions enviar(String aviso) throws Exception {
        return http.perform(post("/internal/avisos")
                        .header(HttpHeaders.AUTHORIZATION, COMO_NUCLEO)
                        .contentType(MediaType.APPLICATION_JSON).content(aviso));
    }

    private AvisoNucleoCommand avisoRecibido() {
        ArgumentCaptor<AvisoNucleoCommand> aviso = ArgumentCaptor.forClass(AvisoNucleoCommand.class);
        verify(aplicarAviso).aplicar(aviso.capture());
        return aviso.getValue();
    }
}
