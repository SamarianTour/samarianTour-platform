package co.edu.unimagdalena.samariantour.catalogo.catalogo.infraestructura.entrada.rest;

import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.excepcion.OfertaNoEncontradaException;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.NivelEsfuerzo;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.Oferta;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.OfertaVisible;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.Pagina;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.Paginacion;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.TipoOferta;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.TipoPrestador;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.puerto.entrada.ConsultarOfertaInternaUseCase;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.puerto.entrada.ConsultarOfertasVisiblesUseCase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.DatosDePrueba.AHORA;
import static co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.DatosDePrueba.PRESTADOR;
import static co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.DatosDePrueba.ZONA_ACTIVA;
import static co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.DatosDePrueba.actividadPublicada;
import static co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.DatosDePrueba.habilitado;
import static co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.DatosDePrueba.ofertaPublicada;
import static co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.DatosDePrueba.paquete;
import static co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.DatosDePrueba.restaurante;
import static co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.DatosDePrueba.zonaActiva;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ConsultaInternaController.class)
class ConsultaInternaControllerTest {

    @Autowired
    private MockMvc http;

    @MockitoBean
    private ConsultarOfertaInternaUseCase consultarOferta;

    @MockitoBean
    private ConsultarOfertasVisiblesUseCase consultarVisibles;

    @Test
    @DisplayName("la consulta del núcleo trae lo que la solicitud copia y lo que valida (RF-30), sin pedir identidad")
    void ofertaInterna() throws Exception {
        when(consultarOferta.consultar("oferta-1")).thenReturn(ofertaPublicada());

        http.perform(get("/internal/ofertas/oferta-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("oferta-1"))
                .andExpect(jsonPath("$.tipo").value("ACTIVIDAD"))
                .andExpect(jsonPath("$.nombre").value("Caminata a Cabo San Juan"))
                .andExpect(jsonPath("$.prestadorId").value(PRESTADOR.toString()))
                .andExpect(jsonPath("$.zonaId").value(ZONA_ACTIVA))
                .andExpect(jsonPath("$.horariosSalida[0]").value("07:00"))
                .andExpect(jsonPath("$.capacidadMaxima").value(12))
                .andExpect(jsonPath("$.precioReferencia").doesNotExist());
    }

    @Test
    @DisplayName("un restaurante no tiene horarios de salida")
    void ofertaInternaSinHorariosDeSalida() throws Exception {
        when(consultarOferta.consultar("restaurante-1")).thenReturn(restaurantePublicado());

        http.perform(get("/internal/ofertas/restaurante-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tipo").value("RESTAURANTE"))
                .andExpect(jsonPath("$.horariosSalida").doesNotExist());
    }

    @Test
    @DisplayName("una oferta inexistente o no visible responde 404 OFERTA_NO_ENCONTRADA")
    void ofertaInternaNoEncontrada() throws Exception {
        when(consultarOferta.consultar("no-existe")).thenThrow(new OfertaNoEncontradaException("no-existe"));

        http.perform(get("/internal/ofertas/no-existe"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.codigo").value("OFERTA_NO_ENCONTRADA"));
    }

    @Test
    @DisplayName("las ofertas visibles traen los datos de los criterios de la recomendación y nada del prestador más "
            + "que su id (RET-02)")
    void visibles() throws Exception {
        Oferta caminata = actividadPublicada("caminata", NivelEsfuerzo.BAJO, true);
        Oferta buceo = actividadPublicada("buceo", NivelEsfuerzo.ALTA_EXIGENCIA, false);
        Oferta paquete = Oferta.publicar("paquete-1", habilitado(), TipoPrestador.AGENCIA_OPERADORA,
                paquete("caminata", "buceo"), zonaActiva(), List.of(caminata, buceo), AHORA);
        paquete.aprobar(1, habilitado(), AHORA);
        when(consultarVisibles.consultar(TipoOferta.PAQUETE, ZONA_ACTIVA, new Paginacion(0, 50))).thenReturn(
                new Pagina<>(List.of(new OfertaVisible(paquete, true, List.of(caminata, buceo))),
                        new Paginacion(0, 50), 1));

        http.perform(get("/internal/ofertas")
                        .param("tipo", "PAQUETE").param("zonaId", ZONA_ACTIVA).param("page", "0").param("size", "50"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contenido", hasSize(1)))
                .andExpect(jsonPath("$.contenido[0].id").value("paquete-1"))
                .andExpect(jsonPath("$.contenido[0].tipo").value("PAQUETE"))
                .andExpect(jsonPath("$.contenido[0].nombre").value("Tayrona en dos días"))
                .andExpect(jsonPath("$.contenido[0].descripcion").isString())
                .andExpect(jsonPath("$.contenido[0].prestadorId").value(PRESTADOR.toString()))
                .andExpect(jsonPath("$.contenido[0].zonaId").value(ZONA_ACTIVA))
                .andExpect(jsonPath("$.contenido[0].zonaAltaAfluencia").value(true))
                .andExpect(jsonPath("$.contenido[0].precioReferencia").value(120000))
                .andExpect(jsonPath("$.contenido[0].unidadPrecio").isString())
                .andExpect(jsonPath("$.contenido[0].tiposActividad", hasSize(1)))
                .andExpect(jsonPath("$.contenido[0].tiposActividad[0]").value("SENDERISMO"))
                .andExpect(jsonPath("$.contenido[0].nivelEsfuerzo").value("ALTA_EXIGENCIA"))
                .andExpect(jsonPath("$.contenido[0].accesibleSillaRuedas").value(false))
                .andExpect(jsonPath("$.totalElementos").value(1));
    }

    @Test
    @DisplayName("sin parámetros no filtra y usa la paginación por defecto; un restaurante no lleva esfuerzo")
    void visiblesSinFiltros() throws Exception {
        when(consultarVisibles.consultar(any(), any(), any())).thenReturn(new Pagina<>(
                List.of(new OfertaVisible(restaurantePublicado(), false, List.of())), Paginacion.de(null, null), 1));

        http.perform(get("/internal/ofertas"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contenido[0].tipo").value("RESTAURANTE"))
                .andExpect(jsonPath("$.contenido[0].tiposActividad", hasSize(0)))
                .andExpect(jsonPath("$.contenido[0].nivelEsfuerzo").doesNotExist())
                .andExpect(jsonPath("$.contenido[0].accesibleSillaRuedas").value(true));

        verify(consultarVisibles).consultar(null, null, new Paginacion(0, 20));
    }

    @Test
    @DisplayName("un tipo desconocido o un tamaño fuera de rango responden 400 DATO_INVALIDO")
    void parametrosInvalidos() throws Exception {
        http.perform(get("/internal/ofertas").param("tipo", "HOTEL"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("DATO_INVALIDO"))
                .andExpect(jsonPath("$.detail", containsString("ALOJAMIENTO")));
        http.perform(get("/internal/ofertas").param("size", "500"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("DATO_INVALIDO"));
    }

    private static Oferta restaurantePublicado() {
        Oferta oferta = Oferta.publicar("restaurante-1", habilitado(), TipoPrestador.RESTAURANTE, restaurante(),
                zonaActiva(), AHORA);
        oferta.aprobar(1, habilitado(), AHORA);
        return oferta;
    }
}
