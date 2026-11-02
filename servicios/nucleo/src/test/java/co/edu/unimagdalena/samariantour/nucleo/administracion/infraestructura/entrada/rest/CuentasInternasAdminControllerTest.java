package co.edu.unimagdalena.samariantour.nucleo.administracion.infraestructura.entrada.rest;

import co.edu.unimagdalena.samariantour.nucleo.administracion.dominio.excepcion.CorreoNoDisponibleException;
import co.edu.unimagdalena.samariantour.nucleo.administracion.dominio.excepcion.CorreoYaRegistradoException;
import co.edu.unimagdalena.samariantour.nucleo.administracion.dominio.excepcion.CuentaInternaNoEncontradaException;
import co.edu.unimagdalena.samariantour.nucleo.administracion.dominio.excepcion.CuentaPropiaException;
import co.edu.unimagdalena.samariantour.nucleo.administracion.dominio.excepcion.OperadorConPrestadoresException;
import co.edu.unimagdalena.samariantour.nucleo.administracion.dominio.modelo.CuentaInterna;
import co.edu.unimagdalena.samariantour.nucleo.administracion.dominio.puerto.entrada.GestionarCuentasInternasUseCase;
import co.edu.unimagdalena.samariantour.nucleo.administracion.dominio.puerto.entrada.comando.CrearCuentaInternaCommand;
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

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Las cuentas internas del administrador (RF-01, RF-81, DN-40). */
@WebMvcTest(CuentasInternasAdminController.class)
@ActiveProfiles("demo")
class CuentasInternasAdminControllerTest {

    private static final UUID ADMINISTRADOR = UUID.fromString("60000000-0000-0000-0000-000000000006");
    private static final UUID OPERADOR = UUID.fromString("60000000-0000-0000-0000-000000000001");
    private static final Instant AHORA = Instant.parse("2026-11-01T15:00:00Z");

    @Autowired
    private MockMvc http;

    @MockitoBean
    private GestionarCuentasInternasUseCase cuentas;

    @Test
    @DisplayName("crear responde 201 con la cuenta sin contraseña todavía")
    void crear() throws Exception {
        when(cuentas.crear(any())).thenReturn(cuenta("ACTIVA", false));

        http.perform(comoAdministrador(post("/admin/cuentas-internas")).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"rol": "OPERADOR_DISPONIBILIDAD", "nombre": "Valentina Ríos",
                                 "correo": "valentina@samariantour.co"}"""))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(OPERADOR.toString()))
                .andExpect(jsonPath("$.estado").value("ACTIVA"))
                .andExpect(jsonPath("$.contrasenaEstablecida").value(false))
                .andExpect(jsonPath("$.registradaEn").value(AHORA.toString()));

        verify(cuentas).crear(new CrearCuentaInternaCommand("OPERADOR_DISPONIBILIDAD", "Valentina Ríos",
                "valentina@samariantour.co"));
    }

    @Test
    @DisplayName("el correo repetido es 409 y el servidor de correo caído, 503")
    void erroresAlCrear() throws Exception {
        when(cuentas.crear(any())).thenThrow(new CorreoYaRegistradoException("Ya hay una cuenta con ese correo"));
        http.perform(crearGestor()).andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("CORREO_YA_REGISTRADO"));

        doThrow(new CorreoNoDisponibleException("No se pudo enviar el correo")).when(cuentas).crear(any());
        http.perform(crearGestor()).andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.codigo").value("CORREO_NO_DISPONIBLE"));
    }

    @Test
    @DisplayName("la lista no lleva contraseñas")
    void consultar() throws Exception {
        when(cuentas.consultar()).thenReturn(List.of(cuenta("ACTIVA", true)));

        http.perform(comoAdministrador(get("/admin/cuentas-internas")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].rol").value("OPERADOR_DISPONIBILIDAD"))
                .andExpect(jsonPath("$[0].hashContrasena").doesNotExist());
    }

    @Test
    @DisplayName("desactivar entrega el administrador de la identidad, que no puede ser la cuenta misma")
    void desactivar() throws Exception {
        when(cuentas.desactivar(OPERADOR, ADMINISTRADOR)).thenReturn(cuenta("DESACTIVADA", true));

        http.perform(comoAdministrador(post("/admin/cuentas-internas/{id}/desactivacion", OPERADOR)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("DESACTIVADA"));

        when(cuentas.desactivar(ADMINISTRADOR, ADMINISTRADOR)).thenThrow(new CuentaPropiaException());
        http.perform(comoAdministrador(post("/admin/cuentas-internas/{id}/desactivacion", ADMINISTRADOR)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("CUENTA_PROPIA"));
    }

    @Test
    @DisplayName("un operador con prestadores es 409; una cuenta que no es interna, 404")
    void erroresAlDesactivar() throws Exception {
        when(cuentas.desactivar(OPERADOR, ADMINISTRADOR)).thenThrow(new OperadorConPrestadoresException(OPERADOR, 2));
        http.perform(comoAdministrador(post("/admin/cuentas-internas/{id}/desactivacion", OPERADOR)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("OPERADOR_CON_PRESTADORES"))
                .andExpect(jsonPath("$.detail", containsString("tiene 2 prestadores asignados")));

        when(cuentas.reactivar(OPERADOR)).thenThrow(new CuentaInternaNoEncontradaException(OPERADOR));
        http.perform(comoAdministrador(post("/admin/cuentas-internas/{id}/reactivacion", OPERADOR)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.codigo").value("CUENTA_NO_ENCONTRADA"));
    }

    @Test
    @DisplayName("solo el administrador: un gestor no crea cuentas")
    void soloElAdministrador() throws Exception {
        http.perform(get("/admin/cuentas-internas")
                        .header(HttpHeaders.AUTHORIZATION, TokensDePrueba.bearer(UUID.randomUUID(), "GESTOR_DESTINO")))
                .andExpect(status().isForbidden());
        verifyNoInteractions(cuentas);
    }

    private static CuentaInterna cuenta(String estado, boolean conContrasena) {
        return new CuentaInterna(OPERADOR, "OPERADOR_DISPONIBILIDAD", "Valentina Ríos", "valentina@samariantour.co",
                estado, conContrasena, AHORA);
    }

    private static MockHttpServletRequestBuilder crearGestor() {
        return comoAdministrador(post("/admin/cuentas-internas")).contentType(MediaType.APPLICATION_JSON)
                .content("{\"rol\": \"GESTOR_DESTINO\", \"nombre\": \"Gestor\", \"correo\": \"g@samariantour.co\"}");
    }

    private static MockHttpServletRequestBuilder comoAdministrador(MockHttpServletRequestBuilder peticion) {
        return peticion.header(HttpHeaders.AUTHORIZATION, TokensDePrueba.bearer(ADMINISTRADOR, "ADMINISTRADOR"));
    }
}
