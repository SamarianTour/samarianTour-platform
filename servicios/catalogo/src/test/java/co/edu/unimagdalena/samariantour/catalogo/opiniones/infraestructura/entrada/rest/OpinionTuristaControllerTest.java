package co.edu.unimagdalena.samariantour.catalogo.opiniones.infraestructura.entrada.rest;

import co.edu.unimagdalena.samariantour.catalogo.opiniones.dominio.excepcion.DatoInvalidoException;
import co.edu.unimagdalena.samariantour.catalogo.opiniones.dominio.excepcion.OfertaNoEncontradaException;
import co.edu.unimagdalena.samariantour.catalogo.opiniones.dominio.excepcion.OpinionDuplicadaException;
import co.edu.unimagdalena.samariantour.catalogo.opiniones.dominio.excepcion.OpinionNoHabilitadaException;
import co.edu.unimagdalena.samariantour.catalogo.opiniones.dominio.modelo.Opinion;
import co.edu.unimagdalena.samariantour.catalogo.opiniones.dominio.modelo.OpinionConOferta;
import co.edu.unimagdalena.samariantour.catalogo.opiniones.dominio.modelo.OpinionesDeOferta;
import co.edu.unimagdalena.samariantour.catalogo.opiniones.dominio.modelo.Pagina;
import co.edu.unimagdalena.samariantour.catalogo.opiniones.dominio.modelo.Paginacion;
import co.edu.unimagdalena.samariantour.catalogo.opiniones.dominio.modelo.ResumenCalificaciones;
import co.edu.unimagdalena.samariantour.catalogo.opiniones.dominio.puerto.entrada.LeerOpinionesOfertaUseCase;
import co.edu.unimagdalena.samariantour.catalogo.opiniones.dominio.puerto.entrada.ListarOpinionesTuristaUseCase;
import co.edu.unimagdalena.samariantour.catalogo.opiniones.dominio.puerto.entrada.PublicarOpinionUseCase;
import co.edu.unimagdalena.samariantour.catalogo.opiniones.dominio.puerto.entrada.comando.PublicarOpinionCommand;
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

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest({OpinionTuristaController.class, OpinionPublicaController.class})
@ActiveProfiles("demo")
class OpinionTuristaControllerTest {

    private static final UUID LAURA = UUID.fromString("70000000-0000-0000-0000-000000000007");
    private static final String CAMINATA = "b00000000000000000000001";
    private static final Instant ENVIADA = Instant.parse("2026-10-21T15:00:00Z");
    private static final Instant REVISADA = Instant.parse("2026-10-22T09:00:00Z");
    private static final String COMENTARIO = "El guía conocía muy bien el sendero y cuidó el grupo.";
    private static final String CUERPO = """
            {"ofertaId": "%s", "calificacion": 5, "comentario": "%s"}
            """.formatted(CAMINATA, COMENTARIO);

    @Autowired
    private MockMvc http;

    @MockitoBean
    private PublicarOpinionUseCase publicar;

    @MockitoBean
    private ListarOpinionesTuristaUseCase listar;

    @MockitoBean
    private LeerOpinionesOfertaUseCase leer;

    private static Opinion opinion() {
        return Opinion.enviar("d00000000000000000000001", CAMINATA, LAURA, 5, COMENTARIO, ENVIADA);
    }

    private static MockHttpServletRequestBuilder comoLaura(MockHttpServletRequestBuilder peticion) {
        return peticion.header(HttpHeaders.AUTHORIZATION, TokensDePrueba.bearer(LAURA.toString(), "TURISTA"));
    }

    @Test
    @DisplayName("el turista publica su opinión con su identidad, no con un turista del cuerpo: 201 en moderación")
    void publicar() throws Exception {
        when(publicar.publicar(new PublicarOpinionCommand(LAURA, CAMINATA, 5, COMENTARIO)))
                .thenReturn(new OpinionConOferta(opinion(), "Caminata a Cabo San Juan"));

        http.perform(comoLaura(post("/turista/opiniones")).contentType(MediaType.APPLICATION_JSON).content(CUERPO))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value("d00000000000000000000001"))
                .andExpect(jsonPath("$.ofertaId").value(CAMINATA))
                .andExpect(jsonPath("$.nombreOferta").value("Caminata a Cabo San Juan"))
                .andExpect(jsonPath("$.calificacion").value(5))
                .andExpect(jsonPath("$.estado").value("EN_MODERACION"))
                .andExpect(jsonPath("$.fecha").value(ENVIADA.toString()))
                .andExpect(jsonPath("$.motivoRechazo").doesNotExist())
                .andExpect(jsonPath("$.revisadaEn").doesNotExist());
    }

    @Test
    @DisplayName("cada regla incumplida sale con su código: 400, 403, 404 y 409")
    void errores() throws Exception {
        when(publicar.publicar(any()))
                .thenThrow(new DatoInvalidoException("comentario", "debe tener entre 10 y 1000 caracteres"))
                .thenThrow(new OpinionNoHabilitadaException(LAURA, CAMINATA))
                .thenThrow(new OfertaNoEncontradaException(CAMINATA))
                .thenThrow(new OpinionDuplicadaException(LAURA, CAMINATA));

        for (Object[] esperado : new Object[][] {{400, "DATO_INVALIDO"}, {403, "OPINION_NO_HABILITADA"},
                {404, "OFERTA_NO_ENCONTRADA"}, {409, "OPINION_DUPLICADA"}}) {
            http.perform(comoLaura(post("/turista/opiniones")).contentType(MediaType.APPLICATION_JSON).content(CUERPO))
                    .andExpect(status().is((int) esperado[0]))
                    .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                    .andExpect(jsonPath("$.codigo").value(esperado[1]));
        }
    }

    @Test
    @DisplayName("un JSON ilegible responde 400 DATO_INVALIDO, como en el catálogo")
    void jsonIlegible() throws Exception {
        http.perform(comoLaura(post("/turista/opiniones")).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"calificacion\": \"cinco\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("DATO_INVALIDO"));
        verifyNoInteractions(publicar);
    }

    @Test
    @DisplayName("solo el turista opina: sin identidad 401 y con otro rol 403 (DC-13)")
    void soloTurista() throws Exception {
        http.perform(post("/turista/opiniones").contentType(MediaType.APPLICATION_JSON).content(CUERPO))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.codigo").value("NO_AUTENTICADO"));
        http.perform(post("/turista/opiniones").contentType(MediaType.APPLICATION_JSON).content(CUERPO)
                        .header(HttpHeaders.AUTHORIZATION, TokensDePrueba.bearer(LAURA.toString(), "ADMINISTRADOR")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.codigo").value("ACCESO_DENEGADO"));

        verifyNoInteractions(publicar);
    }

    @Test
    @DisplayName("mis opiniones trae el estado, el nombre de la oferta y el motivo si fue rechazada")
    void misOpiniones() throws Exception {
        Opinion rechazada = opinion();
        rechazada.rechazar("Incluye datos de contacto", REVISADA);
        when(listar.listar(LAURA, new Paginacion(0, 5))).thenReturn(new Pagina<>(
                List.of(new OpinionConOferta(rechazada, "Caminata a Cabo San Juan")), new Paginacion(0, 5), 1));

        http.perform(comoLaura(get("/turista/opiniones").param("page", "0").param("size", "5")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contenido", hasSize(1)))
                .andExpect(jsonPath("$.contenido[0].estado").value("RECHAZADA"))
                .andExpect(jsonPath("$.contenido[0].motivoRechazo").value("Incluye datos de contacto"))
                .andExpect(jsonPath("$.contenido[0].revisadaEn").value(REVISADA.toString()))
                .andExpect(jsonPath("$.contenido[0].nombreOferta").value("Caminata a Cabo San Juan"))
                .andExpect(jsonPath("$.totalElementos").value(1))
                .andExpect(jsonPath("$.tamano").value(5));
        verify(listar).listar(LAURA, new Paginacion(0, 5));
    }

    @Test
    @DisplayName("la lectura pública no pide identidad, trae el resumen y no muestra el turista")
    void lecturaPublica() throws Exception {
        Opinion aprobada = opinion();
        aprobada.aprobar(REVISADA);
        when(leer.leer(CAMINATA, Paginacion.de(null, null))).thenReturn(new OpinionesDeOferta(
                new Pagina<>(List.of(aprobada), Paginacion.de(null, null), 3), new ResumenCalificaciones(3, 13)));

        http.perform(get("/ofertas/" + CAMINATA + "/opiniones"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resumen.totalOpiniones").value(3))
                .andExpect(jsonPath("$.resumen.promedioCalificacion").value(4.3))
                .andExpect(jsonPath("$.contenido[0].calificacion").value(5))
                .andExpect(jsonPath("$.contenido[0].comentario").value(COMENTARIO))
                .andExpect(jsonPath("$.contenido[0].fecha").value(ENVIADA.toString()))
                .andExpect(jsonPath("$.contenido[0].turistaId").doesNotExist())
                .andExpect(jsonPath("$.contenido[0].estado").doesNotExist())
                .andExpect(jsonPath("$.totalElementos").value(3))
                .andExpect(jsonPath("$.pagina").value(0));
    }

    @Test
    @DisplayName("sin opiniones aprobadas el resumen no trae promedio; una oferta no visible responde 404")
    void lecturaSinOpiniones() throws Exception {
        when(leer.leer(CAMINATA, Paginacion.de(null, null))).thenReturn(new OpinionesDeOferta(
                new Pagina<>(List.of(), Paginacion.de(null, null), 0), ResumenCalificaciones.SIN_OPINIONES));
        when(leer.leer("b00000000000000000000016", Paginacion.de(null, null)))
                .thenThrow(new OfertaNoEncontradaException("b00000000000000000000016"));

        http.perform(get("/ofertas/" + CAMINATA + "/opiniones"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resumen.totalOpiniones").value(0))
                .andExpect(jsonPath("$.resumen.promedioCalificacion").doesNotExist());
        http.perform(get("/ofertas/b00000000000000000000016/opiniones"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.codigo").value("OFERTA_NO_ENCONTRADA"));
        http.perform(get("/ofertas/" + CAMINATA + "/opiniones").param("size", "500"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("DATO_INVALIDO"));
    }
}
