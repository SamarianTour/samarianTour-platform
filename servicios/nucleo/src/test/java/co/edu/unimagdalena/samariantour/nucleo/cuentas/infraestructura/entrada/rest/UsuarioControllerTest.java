package co.edu.unimagdalena.samariantour.nucleo.cuentas.infraestructura.entrada.rest;

import co.edu.unimagdalena.samariantour.nucleo.cuentas.dominio.excepcion.CodigoIncorrectoException;
import co.edu.unimagdalena.samariantour.nucleo.cuentas.dominio.excepcion.CodigoNoVigenteException;
import co.edu.unimagdalena.samariantour.nucleo.cuentas.dominio.excepcion.CuentaNoEncontradaException;
import co.edu.unimagdalena.samariantour.nucleo.cuentas.dominio.modelo.Rol;
import co.edu.unimagdalena.samariantour.nucleo.cuentas.dominio.puerto.entrada.GestionarDatosPersonalesUseCase;
import co.edu.unimagdalena.samariantour.nucleo.cuentas.dominio.puerto.entrada.RegistrarUsuarioUseCase;
import co.edu.unimagdalena.samariantour.nucleo.cuentas.dominio.puerto.entrada.SolicitarSupresionUseCase;
import co.edu.unimagdalena.samariantour.nucleo.cuentas.dominio.puerto.entrada.VerificarWhatsAppUseCase;
import co.edu.unimagdalena.samariantour.nucleo.cuentas.dominio.puerto.entrada.comando.RegistrarUsuarioCommand;
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
import org.springframework.test.web.servlet.ResultActions;

import java.util.UUID;

import static co.edu.unimagdalena.samariantour.nucleo.cuentas.CuentasDePrueba.turista;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Las rutas del registro y del WhatsApp (RF-01 a RF-03, DN-05, DN-28). */
@WebMvcTest(UsuarioController.class)
@ActiveProfiles("demo")
class UsuarioControllerTest {

    private static final UUID TURISTA = UUID.fromString("70000000-0000-0000-0000-000000000007");

    @Autowired
    private MockMvc http;

    @MockitoBean
    private RegistrarUsuarioUseCase registrar;

    @MockitoBean
    private VerificarWhatsAppUseCase verificarWhatsApp;

    @MockitoBean
    private GestionarDatosPersonalesUseCase datosPersonales;

    @MockitoBean
    private SolicitarSupresionUseCase supresion;

    @Test
    @DisplayName("el registro es público, responde 201 y entrega el comando con la aceptación de la política")
    void registro() throws Exception {
        when(registrar.registrar(any())).thenReturn(turista("laura@correo.co", "+573001234567", false));

        http.perform(post("/cuentas/registro").contentType(MediaType.APPLICATION_JSON).content("""
                        {"rol": "TURISTA", "nombre": "Laura Gómez", "correo": "laura@correo.co",
                         "contrasena": "Playa2026", "whatsapp": "+573001234567", "aceptaPolitica": true,
                         "versionPolitica": "v1"}"""))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.whatsappVerificado").value(false));

        verify(registrar).registrar(new RegistrarUsuarioCommand(Rol.TURISTA, "Laura Gómez", "laura@correo.co",
                "Playa2026", "+573001234567", true, "v1"));
    }

    @Test
    @DisplayName("un rol que no se registra solo responde 400 sin llegar al caso de uso")
    void rolInvalido() throws Exception {
        http.perform(post("/cuentas/registro").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"rol\": \"ADMINISTRADOR\", \"aceptaPolitica\": true}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("DATO_INVALIDO"));
        verifyNoInteractions(registrar);
    }

    @Test
    @DisplayName("traduce los errores del código: 400 con los intentos que quedan, 409 vencido y 404 sin cuenta")
    void erroresDelCodigo() throws Exception {
        doThrow(new CodigoIncorrectoException(1)).when(verificarWhatsApp).verificar(eq(TURISTA), anyString());
        verificar().andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("CODIGO_INCORRECTO"))
                .andExpect(jsonPath("$.intentosRestantes").value(1));

        doThrow(new CodigoNoVigenteException()).when(verificarWhatsApp).verificar(eq(TURISTA), anyString());
        verificar().andExpect(status().isConflict()).andExpect(jsonPath("$.codigo").value("CODIGO_NO_VIGENTE"));

        doThrow(new CuentaNoEncontradaException(TURISTA)).when(verificarWhatsApp).verificar(eq(TURISTA), anyString());
        verificar().andExpect(status().isNotFound()).andExpect(jsonPath("$.codigo").value("CUENTA_NO_ENCONTRADA"));
    }

    @Test
    @DisplayName("pedir otro código es solo del turista: 202 para él, 403 para un prestador")
    void soloTurista() throws Exception {
        http.perform(post("/turista/whatsapp/codigo")
                        .header(HttpHeaders.AUTHORIZATION, TokensDePrueba.bearer(TURISTA.toString(), "TURISTA")))
                .andExpect(status().isAccepted());
        verify(verificarWhatsApp).enviarCodigo(TURISTA);

        http.perform(post("/turista/whatsapp/codigo").header(HttpHeaders.AUTHORIZATION,
                        TokensDePrueba.bearerPrestador(UUID.randomUUID(), "GUIA_INDEPENDIENTE")))
                .andExpect(status().isForbidden());
    }

    private ResultActions verificar() throws Exception {
        return http.perform(post("/turista/whatsapp/verificacion")
                .header(HttpHeaders.AUTHORIZATION, TokensDePrueba.bearer(TURISTA.toString(), "TURISTA"))
                .contentType(MediaType.APPLICATION_JSON).content("{\"codigo\": \"123456\"}"));
    }
}
