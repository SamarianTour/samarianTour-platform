package co.edu.unimagdalena.samariantour.nucleo.administracion.infraestructura.entrada.rest;

import co.edu.unimagdalena.samariantour.nucleo.administracion.dominio.excepcion.OperadorNoValidoException;
import co.edu.unimagdalena.samariantour.nucleo.administracion.dominio.excepcion.PrestadorNoRegistradoException;
import co.edu.unimagdalena.samariantour.nucleo.administracion.dominio.modelo.PrestadorRegistrado;
import co.edu.unimagdalena.samariantour.nucleo.administracion.dominio.puerto.entrada.AsignarOperadorPrestadorUseCase;
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

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Los prestadores y su operador, para el administrador (RF-32, RF-81, DN-39). */
@WebMvcTest(PrestadoresAdminController.class)
@ActiveProfiles("demo")
class PrestadoresAdminControllerTest {

    private static final UUID ADMINISTRADOR = UUID.fromString("60000000-0000-0000-0000-000000000006");
    private static final UUID OPERADOR = UUID.fromString("60000000-0000-0000-0000-000000000001");
    private static final UUID AGENCIA = UUID.fromString("50000000-0000-0000-0000-000000000005");
    private static final Instant AHORA = Instant.parse("2026-10-30T14:00:00Z");

    @Autowired
    private MockMvc http;

    @MockitoBean
    private AsignarOperadorPrestadorUseCase asignacion;

    @Test
    @DisplayName("sin operador: pide solo los que faltan y omite el operador vacío")
    void sinOperador() throws Exception {
        when(asignacion.prestadores(true)).thenReturn(List.of(new PrestadorRegistrado(AGENCIA, "910005",
                "TURISMO SIERRA NEVADA LTDA.", "AGENCIA_OPERADORA", "INACTIVO", null, AHORA)));

        http.perform(comoAdministrador(get("/admin/prestadores").param("sinOperador", "true")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(AGENCIA.toString()))
                .andExpect(jsonPath("$[0].numeroRnt").value("910005"))
                .andExpect(jsonPath("$[0].estadoRnt").value("INACTIVO"))
                .andExpect(jsonPath("$[0].registradoEn").value(AHORA.toString()))
                .andExpect(jsonPath("$[0].operadorId").doesNotExist());
    }

    @Test
    @DisplayName("sin el parámetro trae todos")
    void todos() throws Exception {
        when(asignacion.prestadores(false)).thenReturn(List.of());

        http.perform(comoAdministrador(get("/admin/prestadores"))).andExpect(status().isOk());

        verify(asignacion).prestadores(false);
    }

    @Test
    @DisplayName("asignar entrega el prestador de la ruta y el operador del cuerpo")
    void asignar() throws Exception {
        when(asignacion.asignar(AGENCIA, OPERADOR)).thenReturn(new PrestadorRegistrado(AGENCIA, "910005", null,
                "AGENCIA_OPERADORA", "INACTIVO", OPERADOR, AHORA));

        http.perform(asignar(AGENCIA, "{\"operadorId\": \"%s\"}".formatted(OPERADOR)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.operadorId").value(OPERADOR.toString()))
                .andExpect(jsonPath("$.razonSocial").doesNotExist());
    }

    @Test
    @DisplayName("sin operador en el cuerpo es 400; un operador no válido, 409; un prestador sin RNT, 404")
    void errores() throws Exception {
        http.perform(asignar(AGENCIA, "{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("DATO_INVALIDO"));

        when(asignacion.asignar(any(), any())).thenThrow(new OperadorNoValidoException(OPERADOR));
        http.perform(asignar(AGENCIA, "{\"operadorId\": \"%s\"}".formatted(OPERADOR)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("OPERADOR_NO_VALIDO"));

        doThrow(new PrestadorNoRegistradoException(AGENCIA)).when(asignacion).asignar(any(), any());
        http.perform(asignar(AGENCIA, "{\"operadorId\": \"%s\"}".formatted(OPERADOR)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.codigo").value("PRESTADOR_NO_REGISTRADO"));
    }

    @Test
    @DisplayName("solo el administrador: el operador no se asigna prestadores")
    void soloElAdministrador() throws Exception {
        http.perform(get("/admin/prestadores")
                        .header(HttpHeaders.AUTHORIZATION, TokensDePrueba.bearer(OPERADOR, "OPERADOR_DISPONIBILIDAD")))
                .andExpect(status().isForbidden());
        verifyNoInteractions(asignacion);
    }

    private MockHttpServletRequestBuilder asignar(UUID prestadorId, String cuerpo) {
        return comoAdministrador(put("/admin/prestadores/{id}/operador", prestadorId))
                .contentType(MediaType.APPLICATION_JSON).content(cuerpo);
    }

    private static MockHttpServletRequestBuilder comoAdministrador(MockHttpServletRequestBuilder peticion) {
        return peticion.header(HttpHeaders.AUTHORIZATION, TokensDePrueba.bearer(ADMINISTRADOR, "ADMINISTRADOR"));
    }
}
