package co.edu.unimagdalena.samariantour.catalogo.catalogo.infraestructura.entrada.rest;

import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.excepcion.OfertaNoEditableException;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.excepcion.PrestadorNoAutorizadoException;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.excepcion.RntInactivoException;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.excepcion.TipoOfertaNoPermitidoException;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.excepcion.ZonaNoDisponibleException;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.NivelEsfuerzo;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.TipoOferta;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.TipoPrestador;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.VersionActividad;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.VersionPaquete;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.puerto.entrada.GestionarOfertaUseCase;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.puerto.entrada.comando.EditarOfertaCommand;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.puerto.entrada.comando.PublicarOfertaCommand;
import co.edu.unimagdalena.samariantour.catalogo.compartido.seguridad.IdentidadPorHeaders;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.time.LocalTime;

import static co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.DatosDePrueba.PRESTADOR;
import static co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.DatosDePrueba.ZONA_ACTIVA;
import static co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.DatosDePrueba.ofertaEnModeracion;
import static co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.DatosDePrueba.ofertaPublicada;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(OfertaPrestadorController.class)
@Import(IdentidadPorHeaders.class)
@ActiveProfiles("demo")
class OfertaPrestadorControllerTest {

    private static final String ACTIVIDAD = """
            {
              "tipo": "ACTIVIDAD",
              "nombre": "Caminata a la cascada de Marinka",
              "descripcion": "Sendero de montaña con baño en la cascada",
              "precioReferencia": 70000,
              "zonaId": "parque-tayrona",
              "capacidadMaxima": 8,
              "tipoActividad": "SENDERISMO",
              "nivelEsfuerzo": "MODERADO",
              "horariosSalida": ["07:30", "09:00"],
              "duracionMinutos": 180
            }
            """;

    private static final String PAQUETE = """
            {
              "tipo": "PAQUETE",
              "nombre": "Tayrona en dos días",
              "descripcion": "Caminata y snorkel",
              "precioReferencia": 300000,
              "zonaId": "parque-tayrona",
              "capacidadMaxima": 10,
              "actividadIds": ["caminata", "snorkel"],
              "duracionMinutos": 780,
              "horariosSalida": ["07:00"]
            }
            """;

    @Autowired
    private MockMvc http;

    @MockitoBean
    private GestionarOfertaUseCase gestionarOferta;

    @Test
    @DisplayName("publicar responde 201 con la versión 1 en moderación y entrega al caso de uso la identidad y el contenido")
    void publicar() throws Exception {
        when(gestionarOferta.publicar(any())).thenReturn(ofertaEnModeracion());

        http.perform(comoAgencia(post("/prestador/ofertas")).content(ACTIVIDAD))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value("oferta-1"))
                .andExpect(jsonPath("$.tipo").value("ACTIVIDAD"))
                .andExpect(jsonPath("$.retirada").value(false))
                .andExpect(jsonPath("$.publicada").doesNotExist())
                .andExpect(jsonPath("$.pendiente.numero").value(1))
                .andExpect(jsonPath("$.pendiente.estadoModeracion").value("EN_MODERACION"))
                .andExpect(jsonPath("$.pendiente.revisadaEn").doesNotExist());

        ArgumentCaptor<PublicarOfertaCommand> comando = ArgumentCaptor.forClass(PublicarOfertaCommand.class);
        verify(gestionarOferta).publicar(comando.capture());
        assertThat(comando.getValue().solicitante().prestadorId()).isEqualTo(PRESTADOR);
        assertThat(comando.getValue().solicitante().tipoPrestador()).isEqualTo(TipoPrestador.AGENCIA_OPERADORA);
        assertThat(comando.getValue().contenido()).isInstanceOfSatisfying(VersionActividad.class, actividad -> {
            assertThat(actividad.contenido().nombre()).isEqualTo("Caminata a la cascada de Marinka");
            assertThat(actividad.contenido().precioReferencia()).isEqualByComparingTo("70000");
            assertThat(actividad.contenido().zonaId()).isEqualTo(ZONA_ACTIVA);
            assertThat(actividad.nivelEsfuerzo()).isEqualTo(NivelEsfuerzo.MODERADO);
            assertThat(actividad.horariosSalida()).containsExactly(LocalTime.of(7, 30), LocalTime.of(9, 0));
            assertThat(actividad.duracionMinutos()).isEqualTo(180);
            assertThat(actividad.esAccesibleSillaRuedas()).isFalse();
        });
    }

    @Test
    @DisplayName("el tipo del cuerpo decide la versión: un paquete lleva sus actividades")
    void publicarPaquete() throws Exception {
        when(gestionarOferta.publicar(any())).thenReturn(ofertaEnModeracion());

        http.perform(comoAgencia(post("/prestador/ofertas")).content(PAQUETE)).andExpect(status().isCreated());

        ArgumentCaptor<PublicarOfertaCommand> comando = ArgumentCaptor.forClass(PublicarOfertaCommand.class);
        verify(gestionarOferta).publicar(comando.capture());
        assertThat(comando.getValue().contenido()).isInstanceOfSatisfying(VersionPaquete.class, paquete -> {
            assertThat(paquete.actividadIds()).containsExactly("caminata", "snorkel");
            assertThat(paquete.duracionMinutos()).isEqualTo(780);
        });
    }

    @Test
    @DisplayName("sin los headers de identidad responde 401 NO_AUTENTICADO y no llama al caso de uso")
    void sinIdentidad() throws Exception {
        http.perform(post("/prestador/ofertas").contentType(MediaType.APPLICATION_JSON).content(ACTIVIDAD))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.codigo").value("NO_AUTENTICADO"));
        http.perform(post("/prestador/ofertas").contentType(MediaType.APPLICATION_JSON).content(ACTIVIDAD)
                        .header("X-Prestador-Id", "no-es-un-uuid").header("X-Tipo-Prestador", "AGENCIA_OPERADORA"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.codigo").value("NO_AUTENTICADO"));

        verifyNoInteractions(gestionarOferta);
    }

    @Test
    @DisplayName("los datos mal formados responden 400 DATO_INVALIDO y dicen qué se esperaba")
    void datosMalFormados() throws Exception {
        http.perform(comoAgencia(post("/prestador/ofertas")).content(ACTIVIDAD.replace("07:30", "7 am")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("DATO_INVALIDO"))
                .andExpect(jsonPath("$.detail", containsString("HH:mm")));
        http.perform(comoAgencia(post("/prestador/ofertas")).content(ACTIVIDAD.replace("MODERADO", "MEDIO")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail", containsString("ALTA_EXIGENCIA")));
        http.perform(comoAgencia(post("/prestador/ofertas")).content(ACTIVIDAD.replace("\"capacidadMaxima\": 8,", "")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail", containsString("capacidadMaxima")));
        http.perform(comoAgencia(post("/prestador/ofertas")).content("{ esto no es JSON"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.codigo").value("DATO_INVALIDO"));
        http.perform(post("/prestador/ofertas").contentType(MediaType.APPLICATION_JSON).content(ACTIVIDAD)
                        .header("X-Prestador-Id", PRESTADOR.toString()).header("X-Tipo-Prestador", "HOTEL"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail", containsString("tipoPrestador")));

        verifyNoInteractions(gestionarOferta);
    }

    @Test
    @DisplayName("las reglas del dominio se traducen a los códigos y estados de DC-15")
    void erroresDelDominio() throws Exception {
        doThrow(new RntInactivoException(PRESTADOR)).when(gestionarOferta).publicar(any());
        http.perform(comoAgencia(post("/prestador/ofertas")).content(ACTIVIDAD))
                .andExpect(status().isForbidden())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.codigo").value("RNT_INACTIVO"));

        doThrow(new TipoOfertaNoPermitidoException(TipoPrestador.ALOJAMIENTO, TipoOferta.ACTIVIDAD))
                .when(gestionarOferta).publicar(any());
        http.perform(comoAgencia(post("/prestador/ofertas")).content(ACTIVIDAD))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.codigo").value("TIPO_OFERTA_NO_PERMITIDO"));

        doThrow(new ZonaNoDisponibleException("parque-tayrona")).when(gestionarOferta).publicar(any());
        http.perform(comoAgencia(post("/prestador/ofertas")).content(ACTIVIDAD))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("ZONA_NO_DISPONIBLE"));

        doThrow(new PrestadorNoAutorizadoException("oferta-1")).when(gestionarOferta).editar(any());
        http.perform(comoAgencia(put("/prestador/ofertas/oferta-1")).content(ACTIVIDAD))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.codigo").value("PRESTADOR_NO_AUTORIZADO"));

        doThrow(new OfertaNoEditableException("oferta-1")).when(gestionarOferta).editar(any());
        http.perform(comoAgencia(put("/prestador/ofertas/oferta-1")).content(ACTIVIDAD))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("OFERTA_NO_EDITABLE"));
    }

    @Test
    @DisplayName("editar responde 200 y entrega al caso de uso el id de la ruta")
    void editar() throws Exception {
        when(gestionarOferta.editar(any())).thenReturn(ofertaPublicada());

        http.perform(comoAgencia(put("/prestador/ofertas/oferta-1")).content(ACTIVIDAD))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.publicada.numero").value(1))
                .andExpect(jsonPath("$.publicada.estadoModeracion").value("APROBADA"));

        ArgumentCaptor<EditarOfertaCommand> comando = ArgumentCaptor.forClass(EditarOfertaCommand.class);
        verify(gestionarOferta).editar(comando.capture());
        assertThat(comando.getValue().ofertaId()).isEqualTo("oferta-1");
        assertThat(comando.getValue().solicitante().prestadorId()).isEqualTo(PRESTADOR);
    }

    private static MockHttpServletRequestBuilder comoAgencia(MockHttpServletRequestBuilder peticion) {
        return peticion.contentType(MediaType.APPLICATION_JSON)
                .header("X-Prestador-Id", PRESTADOR.toString())
                .header("X-Tipo-Prestador", "AGENCIA_OPERADORA");
    }
}
