package co.edu.unimagdalena.samariantour.nucleo.solicitudes.infraestructura.entrada.rest;

import co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.excepcion.CatalogoNoDisponibleException;
import co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.excepcion.ClaveIdempotenciaReutilizadaException;
import co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.excepcion.PrestadorSinOperadorException;
import co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.excepcion.SolicitudDuplicadaException;
import co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.excepcion.SolicitudNoEncontradaException;
import co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.modelo.DecisionTurista;
import co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.modelo.Pagina;
import co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.modelo.PeticionIdempotente;
import co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.modelo.RespuestaGuardada;
import co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.modelo.ResultadoIdempotente;
import co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.modelo.SolicitudDisponibilidad;
import co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.puerto.entrada.ConsultarSolicitudesTuristaUseCase;
import co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.puerto.entrada.EnviarSolicitudUseCase;
import co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.puerto.entrada.ResponderCondicionesUseCase;
import co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.puerto.entrada.comando.EnviarSolicitudCommand;
import co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.puerto.entrada.comando.ResponderCondicionesCommand;
import co.edu.unimagdalena.samariantour.seguridadjwt.TokensDePrueba;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;

import static co.edu.unimagdalena.samariantour.nucleo.solicitudes.SolicitudesDePrueba.TURISTA;
import static co.edu.unimagdalena.samariantour.nucleo.solicitudes.SolicitudesDePrueba.atendida;
import static co.edu.unimagdalena.samariantour.nucleo.solicitudes.SolicitudesDePrueba.pendiente;
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

/** Las rutas del turista (RF-30 a RF-33, RF-42, RF-43) y su idempotencia (DN-12). */
@WebMvcTest(SolicitudTuristaController.class)
@ExtendWith(OutputCaptureExtension.class)
@Import(RespuestasIdempotentes.class)
@ActiveProfiles("demo")
class SolicitudTuristaControllerTest {

    private static final String CLAVE = "3d6f9a0e-2b1c-4e5f-8a7b-9c0d1e2f3a4b";
    private static final String CUERPO = """
            {"ofertaId": "b00000000000000000000001", "fechaInicio": "2026-10-28", "horaSalida": "07:00",
             "personas": 2, "nota": "Con una niña"}""";

    @Autowired
    private MockMvc http;

    @MockitoBean
    private EnviarSolicitudUseCase enviar;

    @MockitoBean
    private ConsultarSolicitudesTuristaUseCase consultar;

    @MockitoBean
    private ResponderCondicionesUseCase responder;

    @Test
    @DisplayName("enviar: 201 con la solicitud; el comando lleva el turista de la identidad y los datos del cuerpo")
    void enviar() throws Exception {
        SolicitudDisponibilidad solicitud = pendiente();
        when(enviar.enviar(any(), any(), any())).thenAnswer(llamada -> new ResultadoIdempotente(
                llamada.<Function<SolicitudDisponibilidad, RespuestaGuardada>>getArgument(2).apply(solicitud), false));

        http.perform(comoTurista(post("/turista/solicitudes")).header("Idempotency-Key", CLAVE)
                        .contentType(MediaType.APPLICATION_JSON).content(CUERPO))
                .andExpect(status().isCreated())
                .andExpect(header().doesNotExist("Idempotency-Replayed"))
                .andExpect(jsonPath("$.id").value(solicitud.id().toString()))
                .andExpect(jsonPath("$.estado").value("PENDIENTE"))
                .andExpect(jsonPath("$.oferta.nombre").value("Caminata a Cabo San Juan del Guía"))
                .andExpect(jsonPath("$.horaSalida").value("07:00"))
                .andExpect(jsonPath("$.venceEn").value(solicitud.venceEn().toString()))
                .andExpect(jsonPath("$.historial[0].a").value("PENDIENTE"))
                .andExpect(jsonPath("$.condiciones").doesNotExist());

        ArgumentCaptor<EnviarSolicitudCommand> comando = ArgumentCaptor.forClass(EnviarSolicitudCommand.class);
        ArgumentCaptor<PeticionIdempotente> peticion = ArgumentCaptor.forClass(PeticionIdempotente.class);
        verify(enviar).enviar(comando.capture(), peticion.capture(), any());
        assertThat(comando.getValue()).isEqualTo(new EnviarSolicitudCommand(TURISTA, "b00000000000000000000001",
                LocalDate.parse("2026-10-28"), LocalTime.of(7, 0), null, null, null, 2, "Con una niña"));
        assertThat(peticion.getValue().clave()).isEqualTo(UUID.fromString(CLAVE));
        assertThat(peticion.getValue().usuarioId()).isEqualTo(TURISTA);
    }

    @Test
    @DisplayName("enviar: los campos de alojamiento y de restaurante llegan al comando (RF-30)")
    void camposPorTipo() throws Exception {
        SolicitudDisponibilidad solicitud = pendiente();
        when(enviar.enviar(any(), any(), any())).thenAnswer(llamada -> new ResultadoIdempotente(
                llamada.<Function<SolicitudDisponibilidad, RespuestaGuardada>>getArgument(2).apply(solicitud), false));

        http.perform(comoTurista(post("/turista/solicitudes")).header("Idempotency-Key", CLAVE)
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {"ofertaId": "b0000000000000000000000f", "fechaInicio": "2026-10-28",
                                 "fechaSalida": "2026-10-30", "numeroUnidades": 1, "personas": 3}"""))
                .andExpect(status().isCreated());
        http.perform(comoTurista(post("/turista/solicitudes")).header("Idempotency-Key", UUID.randomUUID().toString())
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {"ofertaId": "b00000000000000000000013", "fechaInicio": "2026-10-28",
                                 "horaReserva": "19:30", "personas": 4}"""))
                .andExpect(status().isCreated());

        ArgumentCaptor<EnviarSolicitudCommand> comando = ArgumentCaptor.forClass(EnviarSolicitudCommand.class);
        verify(enviar, org.mockito.Mockito.times(2)).enviar(comando.capture(), any(), any());
        assertThat(comando.getAllValues()).containsExactly(
                new EnviarSolicitudCommand(TURISTA, "b0000000000000000000000f", LocalDate.parse("2026-10-28"), null,
                        null, LocalDate.parse("2026-10-30"), 1, 3, null),
                new EnviarSolicitudCommand(TURISTA, "b00000000000000000000013", LocalDate.parse("2026-10-28"), null,
                        LocalTime.of(19, 30), null, null, 4, null));
    }

    @Test
    @DisplayName("prestador sin operador: 409 y la incidencia queda en el log (RF-32)")
    void prestadorSinOperador(CapturedOutput salida) throws Exception {
        when(enviar.enviar(any(), any(), any())).thenThrow(new PrestadorSinOperadorException(
                UUID.fromString("30000000-0000-0000-0000-000000000003"), "b0000000000000000000000f"));

        http.perform(comoTurista(post("/turista/solicitudes")).header("Idempotency-Key", CLAVE)
                        .contentType(MediaType.APPLICATION_JSON).content(CUERPO))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("PRESTADOR_SIN_OPERADOR"));

        assertThat(salida.getOut()).contains("[INCIDENCIA RF-32]", "30000000-0000-0000-0000-000000000003",
                "b0000000000000000000000f");
    }

    @Test
    @DisplayName("una respuesta repetida sale tal cual se guardó, con Idempotency-Replayed: true")
    void repetida() throws Exception {
        when(enviar.enviar(any(), any(), any())).thenReturn(new ResultadoIdempotente(
                new RespuestaGuardada(201, "{\"id\":\"original\"}"), true));

        http.perform(comoTurista(post("/turista/solicitudes")).header("Idempotency-Key", CLAVE)
                        .contentType(MediaType.APPLICATION_JSON).content(CUERPO))
                .andExpect(status().isCreated())
                .andExpect(header().string("Idempotency-Replayed", "true"))
                .andExpect(jsonPath("$.id").value("original"));
    }

    @Test
    @DisplayName("el mismo cuerpo con otros espacios da la misma huella: no importa cómo se escribió el JSON")
    void mismaHuella() throws Exception {
        when(enviar.enviar(any(), any(), any())).thenReturn(new ResultadoIdempotente(
                new RespuestaGuardada(201, "{}"), false));
        ArgumentCaptor<PeticionIdempotente> peticion = ArgumentCaptor.forClass(PeticionIdempotente.class);

        http.perform(comoTurista(post("/turista/solicitudes")).header("Idempotency-Key", CLAVE)
                .contentType(MediaType.APPLICATION_JSON).content(CUERPO));
        http.perform(comoTurista(post("/turista/solicitudes")).header("Idempotency-Key", CLAVE)
                .contentType(MediaType.APPLICATION_JSON).content(CUERPO.replace("\n", " ").replace(": ", ":")));

        verify(enviar, org.mockito.Mockito.times(2)).enviar(any(), peticion.capture(), any());
        assertThat(peticion.getAllValues().get(0).huella()).isEqualTo(peticion.getAllValues().get(1).huella());
    }

    @Test
    @DisplayName("sin Idempotency-Key, o con una que no es UUID: 400 DATO_INVALIDO, sin llamar al caso de uso")
    void sinClave() throws Exception {
        http.perform(comoTurista(post("/turista/solicitudes")).contentType(MediaType.APPLICATION_JSON).content(CUERPO))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("DATO_INVALIDO"))
                .andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.startsWith("Idempotency-Key:")));
        http.perform(comoTurista(post("/turista/solicitudes")).header("Idempotency-Key", "abc")
                        .contentType(MediaType.APPLICATION_JSON).content(CUERPO))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(enviar);
    }

    @Test
    @DisplayName("los errores del dominio con su código: 409, 422, 503 y 404")
    void errores() throws Exception {
        when(enviar.enviar(any(), any(), any()))
                .thenThrow(new SolicitudDuplicadaException())
                .thenThrow(new ClaveIdempotenciaReutilizadaException(UUID.fromString(CLAVE)))
                .thenThrow(new CatalogoNoDisponibleException("no respondió a tiempo"));
        UUID ajena = UUID.randomUUID();
        when(consultar.consultar(TURISTA, ajena)).thenThrow(new SolicitudNoEncontradaException(ajena));

        enviarYEsperar(409, "SOLICITUD_DUPLICADA");
        enviarYEsperar(422, "CLAVE_IDEMPOTENCIA_REUTILIZADA");
        enviarYEsperar(503, "CATALOGO_NO_DISPONIBLE");
        http.perform(comoTurista(get("/turista/solicitudes/{id}", ajena)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.codigo").value("SOLICITUD_NO_ENCONTRADA"));
    }

    @Test
    @DisplayName("listar: la página de sus solicitudes, sin el historial")
    void listar() throws Exception {
        SolicitudDisponibilidad solicitud = atendida();
        when(consultar.listar(eq(TURISTA), any())).thenReturn(new Pagina<>(List.of(solicitud), 0, 20, 1));

        http.perform(comoTurista(get("/turista/solicitudes")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contenido[0].estado").value("ATENDIDA"))
                .andExpect(jsonPath("$.contenido[0].historial").doesNotExist())
                .andExpect(jsonPath("$.totalElementos").value(1))
                .andExpect(jsonPath("$.totalPaginas").value(1));
    }

    @Test
    @DisplayName("consultar: con las condiciones, el plazo vigente y el historial (RF-33, RF-42)")
    void consultar() throws Exception {
        SolicitudDisponibilidad solicitud = atendida();
        when(consultar.consultar(TURISTA, solicitud.id())).thenReturn(solicitud);

        http.perform(comoTurista(get("/turista/solicitudes/{id}", solicitud.id())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.condiciones.precioBase").value(300000))
                .andExpect(jsonPath("$.condiciones.politicaCancelacion").value("Sin reembolso a menos de 24 h"))
                .andExpect(jsonPath("$.venceEn").value(solicitud.venceEn().toString()))
                .andExpect(jsonPath("$.historial[1].de").value("PENDIENTE"))
                .andExpect(jsonPath("$.historial[1].a").value("ATENDIDA"))
                .andExpect(jsonPath("$.historial[1].actor").value("OPERADOR"));
    }

    @Test
    @DisplayName("responder: la decisión del cuerpo y la clave; una decisión desconocida es 400")
    void responder() throws Exception {
        SolicitudDisponibilidad solicitud = atendida();
        when(responder.responder(any(), any(), any())).thenAnswer(llamada -> new ResultadoIdempotente(
                llamada.<Function<SolicitudDisponibilidad, RespuestaGuardada>>getArgument(2).apply(solicitud), false));

        http.perform(comoTurista(post("/turista/solicitudes/{id}/respuesta", solicitud.id()))
                        .header("Idempotency-Key", CLAVE).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"decision\": \"CONFIRMAR\"}"))
                .andExpect(status().isOk());
        http.perform(comoTurista(post("/turista/solicitudes/{id}/respuesta", solicitud.id()))
                        .header("Idempotency-Key", CLAVE).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"decision\": \"QUIZAS\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("DATO_INVALIDO"));

        ArgumentCaptor<ResponderCondicionesCommand> comando =
                ArgumentCaptor.forClass(ResponderCondicionesCommand.class);
        verify(responder).responder(comando.capture(), any(), any());
        assertThat(comando.getValue()).isEqualTo(new ResponderCondicionesCommand(TURISTA, solicitud.id(),
                DecisionTurista.CONFIRMAR));
    }

    @Test
    @DisplayName("solo el rol TURISTA: un operador recibe 403 y sin identidad, 401")
    void roles() throws Exception {
        http.perform(get("/turista/solicitudes").header(HttpHeaders.AUTHORIZATION,
                        TokensDePrueba.bearer(UUID.randomUUID(), "OPERADOR_DISPONIBILIDAD")))
                .andExpect(status().isForbidden());
        http.perform(get("/turista/solicitudes")).andExpect(status().isUnauthorized());
    }

    private void enviarYEsperar(int estado, String codigo) throws Exception {
        http.perform(comoTurista(post("/turista/solicitudes")).header("Idempotency-Key", CLAVE)
                        .contentType(MediaType.APPLICATION_JSON).content(CUERPO))
                .andExpect(status().is(estado))
                .andExpect(jsonPath("$.codigo").value(codigo));
    }

    private static MockHttpServletRequestBuilder comoTurista(MockHttpServletRequestBuilder peticion) {
        return peticion.header(HttpHeaders.AUTHORIZATION, TokensDePrueba.bearer(TURISTA.toString(), "TURISTA"));
    }
}
