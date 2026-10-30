package co.edu.unimagdalena.samariantour.catalogo.opiniones.infraestructura.entrada.rest;

import co.edu.unimagdalena.samariantour.catalogo.opiniones.dominio.excepcion.OpinionNoEncontradaException;
import co.edu.unimagdalena.samariantour.catalogo.opiniones.dominio.modelo.Opinion;
import co.edu.unimagdalena.samariantour.catalogo.opiniones.dominio.modelo.OpinionConOferta;
import co.edu.unimagdalena.samariantour.catalogo.opiniones.dominio.modelo.Pagina;
import co.edu.unimagdalena.samariantour.catalogo.opiniones.dominio.modelo.Paginacion;
import co.edu.unimagdalena.samariantour.catalogo.opiniones.dominio.puerto.entrada.AplicarAvisoOpinionUseCase;
import co.edu.unimagdalena.samariantour.catalogo.opiniones.dominio.puerto.entrada.ListarOpinionesPendientesUseCase;
import co.edu.unimagdalena.samariantour.catalogo.opiniones.dominio.puerto.entrada.comando.AvisoHabilitacionOpinion;
import co.edu.unimagdalena.samariantour.catalogo.opiniones.dominio.puerto.entrada.comando.AvisoModeracionOpinion;
import co.edu.unimagdalena.samariantour.catalogo.opiniones.dominio.puerto.entrada.comando.AvisoSupresionTurista;
import co.edu.unimagdalena.samariantour.catalogo.opiniones.dominio.puerto.entrada.comando.DecisionModeracion;
import co.edu.unimagdalena.samariantour.catalogo.opiniones.dominio.puerto.entrada.comando.ResultadoAviso;
import co.edu.unimagdalena.samariantour.seguridadjwt.ServiciosInternos;
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

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest({ModeracionOpinionesController.class, AvisoOpinionesController.class})
@ActiveProfiles("demo")
class ModeracionOpinionesControllerTest {

    /** El token con el que el núcleo llama a las rutas internas (DN-38). */
    private static final String COMO_NUCLEO =
            TokensDePrueba.bearerServicio(ServiciosInternos.NUCLEO, ServiciosInternos.CATALOGO);

    private static final UUID TURISTA = UUID.fromString("80000000-0000-0000-0000-000000000008");
    private static final String ADMINISTRADOR = "60000000-0000-0000-0000-000000000006";
    private static final Instant ENVIADA = Instant.parse("2026-10-19T13:30:00Z");

    @Autowired
    private MockMvc http;

    @MockitoBean
    private ListarOpinionesPendientesUseCase pendientes;

    @MockitoBean
    private AplicarAvisoOpinionUseCase avisos;

    private static Opinion enModeracion() {
        return Opinion.enviar("d0000000000000000000000b", "b00000000000000000000004", TURISTA, 4,
                "Kayaks en buen estado y el instructor estuvo pendiente.", ENVIADA);
    }

    @Test
    @DisplayName("el administrador ve la cola con el nombre de la oferta y sin el turista")
    void colaDelAdministrador() throws Exception {
        when(pendientes.listar(Paginacion.de(null, null))).thenReturn(new Pagina<>(
                List.of(new OpinionConOferta(enModeracion(), "Kayak en la bahía de Taganga")),
                Paginacion.de(null, null), 1));

        http.perform(get("/admin/moderacion/opiniones")
                        .header(HttpHeaders.AUTHORIZATION, TokensDePrueba.bearer(ADMINISTRADOR, "ADMINISTRADOR")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contenido", hasSize(1)))
                .andExpect(jsonPath("$.contenido[0].id").value("d0000000000000000000000b"))
                .andExpect(jsonPath("$.contenido[0].nombreOferta").value("Kayak en la bahía de Taganga"))
                .andExpect(jsonPath("$.contenido[0].calificacion").value(4))
                .andExpect(jsonPath("$.contenido[0].fecha").value(ENVIADA.toString()))
                .andExpect(jsonPath("$.contenido[0].turistaId").doesNotExist())
                .andExpect(jsonPath("$.totalElementos").value(1));
    }

    @Test
    @DisplayName("la cola es solo para el administrador: un turista recibe 403 ACCESO_DENEGADO")
    void soloAdministrador() throws Exception {
        http.perform(get("/admin/moderacion/opiniones")
                        .header(HttpHeaders.AUTHORIZATION, TokensDePrueba.bearer(TURISTA.toString(), "TURISTA")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.codigo").value("ACCESO_DENEGADO"));
        verifyNoInteractions(pendientes);
    }

    @Test
    @DisplayName("el núcleo consulta la opinión que va a decidir, con el turista para notificarle")
    void consultaDelNucleo() throws Exception {
        when(pendientes.consultar("d0000000000000000000000b")).thenReturn(enModeracion());
        when(pendientes.consultar("d00000000000000000000001"))
                .thenThrow(new OpinionNoEncontradaException("d00000000000000000000001"));

        http.perform(get("/internal/moderacion/opiniones/d0000000000000000000000b")
                        .header(HttpHeaders.AUTHORIZATION, COMO_NUCLEO))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.turistaId").value(TURISTA.toString()))
                .andExpect(jsonPath("$.ofertaId").value("b00000000000000000000004"))
                .andExpect(jsonPath("$.estado").value("EN_MODERACION"));
        http.perform(get("/internal/moderacion/opiniones/d00000000000000000000001")
                        .header(HttpHeaders.AUTHORIZATION, COMO_NUCLEO))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.codigo").value("OPINION_NO_ENCONTRADA"));
    }

    @Test
    @DisplayName("cada tipo de aviso llega al caso de uso con sus datos")
    void avisos() throws Exception {
        when(avisos.aplicar(new AvisoHabilitacionOpinion("a1", TURISTA, "b00000000000000000000006")))
                .thenReturn(ResultadoAviso.APLICADO);
        when(avisos.aplicar(new AvisoModeracionOpinion("a2", "d0000000000000000000000b",
                DecisionModeracion.RECHAZAR, "Incluye datos de contacto"))).thenReturn(ResultadoAviso.IGNORADO);
        when(avisos.aplicar(new AvisoSupresionTurista("a3", TURISTA))).thenReturn(ResultadoAviso.DUPLICADO);

        avisar("""
                {"idAviso": "a1", "tipo": "HABILITACION_OPINION", "turistaId": "%s", "ofertaId": "b00000000000000000000006"}
                """.formatted(TURISTA), "APLICADO");
        avisar("""
                {"idAviso": "a2", "tipo": "MODERACION_OPINION", "opinionId": "d0000000000000000000000b",
                 "decision": "RECHAZAR", "motivo": "Incluye datos de contacto"}
                """, "IGNORADO");
        avisar("""
                {"idAviso": "a3", "tipo": "SUPRESION_TURISTA", "turistaId": "%s"}
                """.formatted(TURISTA), "DUPLICADO");
        verify(avisos).aplicar(new AvisoSupresionTurista("a3", TURISTA));
    }

    @Test
    @DisplayName("un aviso mal formado responde 400 DATO_INVALIDO sin llegar al caso de uso")
    void avisoInvalido() throws Exception {
        for (String cuerpo : new String[] {
                "{\"idAviso\": \"a1\", \"tipo\": \"OTRO\"}",
                "{\"idAviso\": \"a1\", \"tipo\": \"SUPRESION_TURISTA\", \"turistaId\": \"no-es-uuid\"}",
                "{\"idAviso\": \"a1\", \"tipo\": \"MODERACION_OPINION\", \"opinionId\": \"d1\", \"decision\": \"RECHAZAR\"}"}) {
            http.perform(post("/internal/opiniones/avisos")
                            .header(HttpHeaders.AUTHORIZATION, COMO_NUCLEO)
                            .contentType(MediaType.APPLICATION_JSON).content(cuerpo))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.codigo").value("DATO_INVALIDO"));
        }
        verifyNoInteractions(avisos);
    }

    private void avisar(String cuerpo, String resultado) throws Exception {
        http.perform(post("/internal/opiniones/avisos")
                        .header(HttpHeaders.AUTHORIZATION, COMO_NUCLEO)
                        .contentType(MediaType.APPLICATION_JSON).content(cuerpo))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultado").value(resultado));
    }
}
