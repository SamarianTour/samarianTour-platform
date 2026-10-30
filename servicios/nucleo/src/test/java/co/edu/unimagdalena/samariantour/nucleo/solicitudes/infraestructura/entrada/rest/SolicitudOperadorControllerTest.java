package co.edu.unimagdalena.samariantour.nucleo.solicitudes.infraestructura.entrada.rest;

import co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.excepcion.SinTiempoParaConfirmarException;
import co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.excepcion.SolicitudNoEncontradaException;
import co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.modelo.AvisoPlataforma;
import co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.modelo.DetalleSolicitudOperador;
import co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.modelo.EstadoSolicitud;
import co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.modelo.Pagina;
import co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.modelo.ResultadoAtencion;
import co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.modelo.SolicitudDisponibilidad;
import co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.modelo.TipoNotificacion;
import co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.puerto.entrada.AtenderSolicitudUseCase;
import co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.puerto.entrada.ConsultarColaOperadorUseCase;
import co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.puerto.entrada.comando.AtenderSolicitudCommand;
import co.edu.unimagdalena.samariantour.seguridadjwt.TokensDePrueba;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static co.edu.unimagdalena.samariantour.nucleo.solicitudes.SolicitudesDePrueba.AHORA;
import static co.edu.unimagdalena.samariantour.nucleo.solicitudes.SolicitudesDePrueba.OPERADOR;
import static co.edu.unimagdalena.samariantour.nucleo.solicitudes.SolicitudesDePrueba.pendiente;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Las rutas del operador de disponibilidad (RF-34 a RF-40). */
@WebMvcTest(SolicitudOperadorController.class)
@Import(SolicitudOperadorControllerTest.RelojFijo.class)
@ActiveProfiles("demo")
class SolicitudOperadorControllerTest {

    /** 19 h después de creada: a la pendiente le quedan 5 h, está próxima a vencer (RF-36). */
    static final Instant AHORA_DEL_OPERADOR = AHORA.plus(Duration.ofHours(19));

    @TestConfiguration
    static class RelojFijo {
        @Bean
        Clock reloj() {
            return Clock.fixed(AHORA_DEL_OPERADOR, ZoneOffset.UTC);
        }
    }

    @Autowired
    private MockMvc http;

    @MockitoBean
    private ConsultarColaOperadorUseCase cola;

    @MockitoBean
    private AtenderSolicitudUseCase atender;

    private final SolicitudDisponibilidad solicitud = pendiente();

    @Test
    @DisplayName("sin estado, su cola de pendientes, con próxima a vencer resaltada")
    void cola() throws Exception {
        when(cola.cola(eq(OPERADOR), eq(EstadoSolicitud.PENDIENTE), any()))
                .thenReturn(new Pagina<>(List.of(solicitud), 0, 20, 1));

        http.perform(comoOperador(get("/operador/solicitudes")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contenido[0].id").value(solicitud.id().toString()))
                .andExpect(jsonPath("$.contenido[0].proximaAVencer").value(true))
                .andExpect(jsonPath("$.contenido[0].venceEn").value(solicitud.venceEn().toString()));
    }

    @Test
    @DisplayName("con ?estado= su historial de trámites; un estado desconocido es 400")
    void historial() throws Exception {
        when(cola.cola(eq(OPERADOR), eq(EstadoSolicitud.ATENDIDA), any()))
                .thenReturn(new Pagina<>(List.of(), 0, 20, 0));

        http.perform(comoOperador(get("/operador/solicitudes").param("estado", "ATENDIDA")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contenido").isEmpty());
        http.perform(comoOperador(get("/operador/solicitudes").param("estado", "ARCHIVADA")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("DATO_INVALIDO"));
    }

    @Test
    @DisplayName("el detalle trae el prestador y su WhatsApp; la de otro operador es 404")
    void detalle() throws Exception {
        when(cola.detalle(OPERADOR, solicitud.id()))
                .thenReturn(new DetalleSolicitudOperador(solicitud, "+573001110001"));
        UUID ajena = UUID.randomUUID();
        when(cola.detalle(OPERADOR, ajena)).thenThrow(new SolicitudNoEncontradaException(ajena));

        http.perform(comoOperador(get("/operador/solicitudes/{id}", solicitud.id())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.prestadorId").value("10000000-0000-0000-0000-000000000001"))
                .andExpect(jsonPath("$.whatsappPrestador").value("+573001110001"))
                .andExpect(jsonPath("$.nota").value("Vamos con una niña de 8 años"))
                .andExpect(jsonPath("$.historial[0].a").value("PENDIENTE"));
        http.perform(comoOperador(get("/operador/solicitudes/{id}", ajena)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.codigo").value("SOLICITUD_NO_ENCONTRADA"));
    }

    @Test
    @DisplayName("atender: el comando con el operador de la identidad, el precio y las condiciones")
    void atender() throws Exception {
        when(cola.detalle(OPERADOR, solicitud.id())).thenReturn(new DetalleSolicitudOperador(solicitud, null));

        http.perform(comoOperador(post("/operador/solicitudes/{id}/atencion", solicitud.id()))
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {"resultado": "ATENDIDA", "precioBase": 300000, "aparteAcordado": true,
                                 "politicaCancelacion": "Sin reembolso a menos de 24 h"}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.whatsappPrestador").doesNotExist());

        ArgumentCaptor<AtenderSolicitudCommand> comando = ArgumentCaptor.forClass(AtenderSolicitudCommand.class);
        verify(atender).atender(comando.capture());
        assertThat(comando.getValue()).isEqualTo(new AtenderSolicitudCommand(OPERADOR, solicitud.id(),
                ResultadoAtencion.ATENDIDA, 300_000L, "Sin reembolso a menos de 24 h", true, null));
    }

    @Test
    @DisplayName("un resultado desconocido es 400 sin llamar al caso de uso; sin tiempo para confirmar, 409")
    void atenderErrores() throws Exception {
        http.perform(comoOperador(post("/operador/solicitudes/{id}/atencion", solicitud.id()))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"resultado\": \"TAL_VEZ\"}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(atender);

        when(atender.atender(any())).thenThrow(new SinTiempoParaConfirmarException(AHORA, AHORA));
        http.perform(comoOperador(post("/operador/solicitudes/{id}/atencion", solicitud.id()))
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {"resultado": "ATENDIDA", "precioBase": 300000, "aparteAcordado": true,
                                 "politicaCancelacion": "Sin reembolso"}"""))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("SIN_TIEMPO_PARA_CONFIRMAR"));
    }

    @Test
    @DisplayName("los avisos del operador (RF-34)")
    void avisos() throws Exception {
        when(cola.avisos(eq(OPERADOR), any())).thenReturn(new Pagina<>(List.of(new AvisoPlataforma(UUID.randomUUID(),
                solicitud.id(), TipoNotificacion.NUEVA_SOLICITUD, Map.of("nombreOferta", "Caminata"), AHORA)), 0, 20,
                1));

        http.perform(comoOperador(get("/operador/avisos")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contenido[0].tipo").value("NUEVA_SOLICITUD"))
                .andExpect(jsonPath("$.contenido[0].datos.nombreOferta").value("Caminata"));
    }

    @Test
    @DisplayName("solo el rol OPERADOR_DISPONIBILIDAD")
    void rol() throws Exception {
        http.perform(get("/operador/solicitudes")
                        .header(HttpHeaders.AUTHORIZATION, TokensDePrueba.bearer(OPERADOR.toString(), "TURISTA")))
                .andExpect(status().isForbidden());
    }

    private static MockHttpServletRequestBuilder comoOperador(MockHttpServletRequestBuilder peticion) {
        return peticion
                .header(HttpHeaders.AUTHORIZATION,
                        TokensDePrueba.bearer(OPERADOR.toString(), "OPERADOR_DISPONIBILIDAD"));
    }
}
