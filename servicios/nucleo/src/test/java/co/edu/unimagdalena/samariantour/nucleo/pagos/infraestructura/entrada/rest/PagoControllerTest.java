package co.edu.unimagdalena.samariantour.nucleo.pagos.infraestructura.entrada.rest;

import co.edu.unimagdalena.samariantour.nucleo.pagos.dominio.excepcion.ClaveIdempotenciaReutilizadaException;
import co.edu.unimagdalena.samariantour.nucleo.pagos.dominio.excepcion.EventoNoAutenticoException;
import co.edu.unimagdalena.samariantour.nucleo.pagos.dominio.excepcion.IntentosAgotadosException;
import co.edu.unimagdalena.samariantour.nucleo.pagos.dominio.excepcion.PagoNoAplicableException;
import co.edu.unimagdalena.samariantour.nucleo.pagos.dominio.excepcion.SolicitudNoEncontradaException;
import co.edu.unimagdalena.samariantour.nucleo.pagos.dominio.modelo.IntentoPago;
import co.edu.unimagdalena.samariantour.nucleo.pagos.dominio.modelo.PeticionIdempotente;
import co.edu.unimagdalena.samariantour.nucleo.pagos.dominio.modelo.RespuestaGuardada;
import co.edu.unimagdalena.samariantour.nucleo.pagos.dominio.modelo.ResultadoIdempotente;
import co.edu.unimagdalena.samariantour.nucleo.pagos.dominio.modelo.SesionCobro;
import co.edu.unimagdalena.samariantour.nucleo.pagos.dominio.modelo.SesionGenerada;
import co.edu.unimagdalena.samariantour.nucleo.pagos.dominio.puerto.entrada.GenerarSesionCobroUseCase;
import co.edu.unimagdalena.samariantour.nucleo.pagos.dominio.puerto.entrada.RecibirEventoPasarelaUseCase;
import co.edu.unimagdalena.samariantour.seguridadjwt.TokensDePrueba;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Las rutas de pagos del turista y el webhook (RF-46, RF-50, ADR-04, DN-19, DN-21). */
@WebMvcTest({PagoController.class, WebhookPasarelaController.class})
@Import(RespuestasIdempotentesPagos.class)
@ActiveProfiles("demo")
class PagoControllerTest {

    private static final UUID TURISTA = UUID.fromString("90000000-0000-0000-0000-000000000001");
    private static final UUID SOLICITUD = UUID.fromString("6f1c2a9e-4b7d-4c1e-9a52-3d8e0f7b1a20");
    private static final String CLAVE = "3d6f9a0e-2b1c-4e5f-8a7b-9c0d1e2f3a4b";
    private static final Instant AHORA = Instant.parse("2026-10-27T15:00:00Z");

    @Autowired
    private MockMvc http;

    @MockitoBean
    private GenerarSesionCobroUseCase cobros;

    @MockitoBean
    private RecibirEventoPasarelaUseCase eventos;

    @Test
    @DisplayName("pago: 201 con el intento y el checkout; la clave y el turista vienen de los headers")
    void generar() throws Exception {
        IntentoPago intento = IntentoPago.nuevo(UUID.randomUUID(), SOLICITUD, TURISTA, 1, 300_000, null, AHORA);
        SesionCobro sesion = new SesionCobro("SIMULADA", "http://localhost:8080/api/nucleo/pasarela-simulada/x", null,
                null, "http://localhost:8080/turista/solicitudes/" + SOLICITUD);
        when(cobros.generar(eq(SOLICITUD), eq(TURISTA), any(), any())).thenAnswer(llamada -> new ResultadoIdempotente(
                llamada.<Function<SesionGenerada, RespuestaGuardada>>getArgument(3)
                        .apply(new SesionGenerada(intento, sesion)), false));

        http.perform(post("/turista/solicitudes/{id}/pago", SOLICITUD)
                        .header(HttpHeaders.AUTHORIZATION, TokensDePrueba.bearer(TURISTA, "TURISTA"))
                        .header("Idempotency-Key", CLAVE))
                .andExpect(status().isCreated())
                .andExpect(header().doesNotExist("Idempotency-Replayed"))
                .andExpect(jsonPath("$.intentoId").value(intento.id().toString()))
                .andExpect(jsonPath("$.numero").value(1))
                .andExpect(jsonPath("$.estado").value("EN_CURSO"))
                .andExpect(jsonPath("$.montoCentavos").value(30_000_000))
                .andExpect(jsonPath("$.moneda").value("COP"))
                .andExpect(jsonPath("$.checkout.pasarela").value("SIMULADA"))
                .andExpect(jsonPath("$.checkout.llavePublica").doesNotExist());

        ArgumentCaptor<PeticionIdempotente> peticion = ArgumentCaptor.forClass(PeticionIdempotente.class);
        verify(cobros).generar(eq(SOLICITUD), eq(TURISTA), peticion.capture(), any());
        assertThat(peticion.getValue().clave()).isEqualTo(UUID.fromString(CLAVE));
        assertThat(peticion.getValue().operacion()).isEqualTo("GENERAR_SESION_COBRO");
    }

    @Test
    @DisplayName("una respuesta repetida sale tal cual, con Idempotency-Replayed: true; sin clave, 400")
    void repetidaYSinClave() throws Exception {
        when(cobros.generar(any(), any(), any(), any())).thenReturn(new ResultadoIdempotente(
                new RespuestaGuardada(201, "{\"intentoId\":\"original\"}"), true));

        http.perform(post("/turista/solicitudes/{id}/pago", SOLICITUD)
                        .header(HttpHeaders.AUTHORIZATION, TokensDePrueba.bearer(TURISTA, "TURISTA"))
                        .header("Idempotency-Key", CLAVE))
                .andExpect(status().isCreated())
                .andExpect(header().string("Idempotency-Replayed", "true"))
                .andExpect(jsonPath("$.intentoId").value("original"));
        http.perform(post("/turista/solicitudes/{id}/pago", SOLICITUD)
                        .header(HttpHeaders.AUTHORIZATION, TokensDePrueba.bearer(TURISTA, "TURISTA"))
                        .header("Idempotency-Key", "no-es-uuid"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("DATO_INVALIDO"));
    }

    @Test
    @DisplayName("los errores: 404 ajena, 409 intentos agotados y plazo vencido, 422 clave reutilizada")
    void errores() throws Exception {
        when(cobros.generar(any(), any(), any(), any()))
                .thenThrow(new SolicitudNoEncontradaException(SOLICITUD))
                .thenThrow(new IntentosAgotadosException())
                .thenThrow(new PagoNoAplicableException("PLAZO_VENCIDO", "terminó la ventana"))
                .thenThrow(new ClaveIdempotenciaReutilizadaException(UUID.fromString(CLAVE)));

        for (String[] esperado : List.of(new String[]{"404", "SOLICITUD_NO_ENCONTRADA"},
                new String[]{"409", "INTENTOS_AGOTADOS"}, new String[]{"409", "PLAZO_VENCIDO"},
                new String[]{"422", "CLAVE_IDEMPOTENCIA_REUTILIZADA"})) {
            http.perform(post("/turista/solicitudes/{id}/pago", SOLICITUD)
                            .header(HttpHeaders.AUTHORIZATION, TokensDePrueba.bearer(TURISTA, "TURISTA"))
                            .header("Idempotency-Key", CLAVE))
                    .andExpect(status().is(Integer.parseInt(esperado[0])))
                    .andExpect(jsonPath("$.codigo").value(esperado[1]));
        }
    }

    @Test
    @DisplayName("solo el turista: otro rol 403 y sin identidad 401, sin llegar al caso de uso")
    void soloElTurista() throws Exception {
        http.perform(post("/turista/solicitudes/{id}/pago", SOLICITUD)
                        .header(HttpHeaders.AUTHORIZATION,
                                TokensDePrueba.bearer(UUID.randomUUID(), "OPERADOR_DISPONIBILIDAD"))
                        .header("Idempotency-Key", CLAVE))
                .andExpect(status().isForbidden());
        http.perform(get("/turista/solicitudes/{id}/pagos", SOLICITUD)).andExpect(status().isUnauthorized());
        verifyNoInteractions(cobros);
    }

    @Test
    @DisplayName("webhook: público; 200 con duplicado false o true; con firma inválida, 401")
    void webhook() throws Exception {
        when(eventos.recibir("{\"event\":\"a\"}", "firma")).thenReturn(true);
        when(eventos.recibir("{\"event\":\"b\"}", null)).thenReturn(false);
        when(eventos.recibir("{\"event\":\"c\"}", null))
                .thenThrow(new EventoNoAutenticoException("la firma no coincide"));

        http.perform(post("/pagos/eventos").contentType(MediaType.APPLICATION_JSON).content("{\"event\":\"a\"}")
                        .header("X-Event-Checksum", "firma"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.duplicado").value(false));
        http.perform(post("/pagos/eventos").contentType(MediaType.APPLICATION_JSON).content("{\"event\":\"b\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.duplicado").value(true));
        http.perform(post("/pagos/eventos").contentType(MediaType.APPLICATION_JSON).content("{\"event\":\"c\"}"))
                .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.codigo").value("EVENTO_NO_AUTENTICO"));
    }
}
