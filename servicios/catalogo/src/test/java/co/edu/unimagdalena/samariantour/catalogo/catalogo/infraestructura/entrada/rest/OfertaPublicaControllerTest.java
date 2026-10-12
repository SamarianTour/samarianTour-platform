package co.edu.unimagdalena.samariantour.catalogo.catalogo.infraestructura.entrada.rest;

import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.excepcion.OfertaNoEncontradaException;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.CriteriosBusqueda;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.FichaOferta;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.NivelEsfuerzo;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.Oferta;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.Pagina;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.Paginacion;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.PerfilPublico;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.TipoActividad;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.TipoOferta;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.TipoPrestador;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.puerto.entrada.BuscarOfertasUseCase;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.puerto.entrada.ConsultarFichaOfertaUseCase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;

import static co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.DatosDePrueba.AHORA;
import static co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.DatosDePrueba.PRESTADOR;
import static co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.DatosDePrueba.ZONA_ACTIVA;
import static co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.DatosDePrueba.actividadPublicada;
import static co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.DatosDePrueba.habilitado;
import static co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.DatosDePrueba.ofertaPublicada;
import static co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.DatosDePrueba.paquete;
import static co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.DatosDePrueba.zonaActiva;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(OfertaPublicaController.class)
class OfertaPublicaControllerTest {

    @Autowired
    private MockMvc http;

    @MockitoBean
    private BuscarOfertasUseCase buscarOfertas;

    @MockitoBean
    private ConsultarFichaOfertaUseCase consultarFicha;

    @Test
    @DisplayName("la búsqueda convierte los parámetros en criterios y devuelve la página con el resumen de cada oferta")
    void buscar() throws Exception {
        when(buscarOfertas.buscar(any())).thenReturn(new Pagina<>(List.of(ofertaPublicada()), new Paginacion(0, 5), 1));

        http.perform(get("/ofertas")
                        .param("tipo", "ACTIVIDAD").param("zonaId", ZONA_ACTIVA)
                        .param("precioMin", "50000").param("precioMax", "200000")
                        .param("tipoActividad", "SENDERISMO").param("nivelEsfuerzoMaximo", "MODERADO")
                        .param("accesibleSillaRuedas", "true").param("page", "0").param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contenido", hasSize(1)))
                .andExpect(jsonPath("$.contenido[0].id").value("oferta-1"))
                .andExpect(jsonPath("$.contenido[0].tipo").value("ACTIVIDAD"))
                .andExpect(jsonPath("$.contenido[0].nombre").value("Caminata a Cabo San Juan"))
                .andExpect(jsonPath("$.contenido[0].precioReferencia").value(120000))
                .andExpect(jsonPath("$.contenido[0].unidadPrecio").value("por persona"))
                .andExpect(jsonPath("$.pagina").value(0))
                .andExpect(jsonPath("$.tamano").value(5))
                .andExpect(jsonPath("$.totalElementos").value(1))
                .andExpect(jsonPath("$.totalPaginas").value(1));

        ArgumentCaptor<CriteriosBusqueda> criterios = ArgumentCaptor.forClass(CriteriosBusqueda.class);
        verify(buscarOfertas).buscar(criterios.capture());
        assertThat(criterios.getValue().tipo()).isEqualTo(TipoOferta.ACTIVIDAD);
        assertThat(criterios.getValue().zonaId()).isEqualTo(ZONA_ACTIVA);
        assertThat(criterios.getValue().precioMin()).isEqualByComparingTo("50000");
        assertThat(criterios.getValue().precioMax()).isEqualByComparingTo("200000");
        assertThat(criterios.getValue().tipoActividad()).isEqualTo(TipoActividad.SENDERISMO);
        assertThat(criterios.getValue().nivelEsfuerzoMaximo()).isEqualTo(NivelEsfuerzo.MODERADO);
        assertThat(criterios.getValue().soloAccesibleSillaRuedas()).isTrue();
        assertThat(criterios.getValue().paginacion()).isEqualTo(new Paginacion(0, 5));
    }

    @Test
    @DisplayName("sin parámetros, la búsqueda no filtra y usa la paginación por defecto (DC-14)")
    void buscarSinFiltros() throws Exception {
        when(buscarOfertas.buscar(any())).thenReturn(new Pagina<>(List.of(), Paginacion.de(null, null), 0));

        http.perform(get("/ofertas")).andExpect(status().isOk()).andExpect(jsonPath("$.contenido", hasSize(0)));

        ArgumentCaptor<CriteriosBusqueda> criterios = ArgumentCaptor.forClass(CriteriosBusqueda.class);
        verify(buscarOfertas).buscar(criterios.capture());
        assertThat(criterios.getValue())
                .isEqualTo(new CriteriosBusqueda(null, null, null, null, null, null, false, new Paginacion(0, 20)));
    }

    @Test
    @DisplayName("un filtro con un valor desconocido responde 400 DATO_INVALIDO con los valores permitidos")
    void filtroDesconocido() throws Exception {
        http.perform(get("/ofertas").param("tipo", "HOTEL"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.codigo").value("DATO_INVALIDO"))
                .andExpect(jsonPath("$.detail", containsString("ALOJAMIENTO")));
    }

    @Test
    @DisplayName("un precio que no es un número y un tamaño fuera de rango responden 400 DATO_INVALIDO")
    void parametrosMalFormados() throws Exception {
        http.perform(get("/ofertas").param("precioMin", "barato"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("DATO_INVALIDO"))
                .andExpect(jsonPath("$.detail", containsString("precioMin")));
        http.perform(get("/ofertas").param("size", "500"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("DATO_INVALIDO"));
    }

    @Test
    @DisplayName("la ficha de una actividad trae su detalle, la zona y el perfil público del prestador")
    void fichaDeActividad() throws Exception {
        PerfilPublico perfil = new PerfilPublico(PRESTADOR, "Agencia local", List.of(), null,
                "https://www.example.com/agencia", List.of());
        when(consultarFicha.consultar("oferta-1")).thenReturn(
                new FichaOferta(ofertaPublicada(), Optional.of(zonaActiva()), Optional.of(perfil), List.of()));

        http.perform(get("/ofertas/oferta-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("oferta-1"))
                .andExpect(jsonPath("$.unidadPrecio").value("por persona"))
                .andExpect(jsonPath("$.capacidadMaxima").value(12))
                .andExpect(jsonPath("$.accesibleSillaRuedas").value(false))
                .andExpect(jsonPath("$.actividad.tipoActividad").value("SENDERISMO"))
                .andExpect(jsonPath("$.actividad.nivelEsfuerzo").value("MODERADO"))
                .andExpect(jsonPath("$.actividad.horariosSalida[0]").value("07:00"))
                .andExpect(jsonPath("$.actividad.puntoEncuentro").doesNotExist())
                .andExpect(jsonPath("$.paquete").doesNotExist())
                .andExpect(jsonPath("$.alojamiento").doesNotExist())
                .andExpect(jsonPath("$.zona.id").value(ZONA_ACTIVA))
                .andExpect(jsonPath("$.zona.areaProtegida").value(true))
                .andExpect(jsonPath("$.zona.buenasPracticas[0]").value("No dejar residuos"))
                .andExpect(jsonPath("$.perfilPrestador.descripcion").value("Agencia local"))
                .andExpect(jsonPath("$.perfilPrestador.sitioWeb").value("https://www.example.com/agencia"));
    }

    @Test
    @DisplayName("la ficha de un paquete calcula su esfuerzo, sus tipos y su accesibilidad con sus actividades (RF-17)")
    void fichaDePaquete() throws Exception {
        Oferta caminata = actividadPublicada("caminata", NivelEsfuerzo.BAJO, true);
        Oferta buceo = actividadPublicada("buceo", NivelEsfuerzo.ALTA_EXIGENCIA, false);
        Oferta paquete = Oferta.publicar("paquete-1", habilitado(), TipoPrestador.AGENCIA_OPERADORA,
                paquete("caminata", "buceo"), zonaActiva(), List.of(caminata, buceo), AHORA);
        paquete.aprobar(1, true, AHORA);
        when(consultarFicha.consultar("paquete-1")).thenReturn(
                new FichaOferta(paquete, Optional.empty(), Optional.empty(), List.of(caminata, buceo)));

        http.perform(get("/ofertas/paquete-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tipo").value("PAQUETE"))
                .andExpect(jsonPath("$.accesibleSillaRuedas").value(false))
                .andExpect(jsonPath("$.paquete.nivelEsfuerzo").value("ALTA_EXIGENCIA"))
                .andExpect(jsonPath("$.paquete.tiposActividad", hasSize(1)))
                .andExpect(jsonPath("$.paquete.tiposActividad[0]").value("SENDERISMO"))
                .andExpect(jsonPath("$.paquete.actividades", hasSize(2)))
                .andExpect(jsonPath("$.paquete.actividades[0].id").value("caminata"))
                .andExpect(jsonPath("$.paquete.actividades[0].nombre").value("Actividad caminata"))
                .andExpect(jsonPath("$.paquete.duracionMinutos").value(600))
                .andExpect(jsonPath("$.paquete.horariosSalida[0]").value("06:30"))
                .andExpect(jsonPath("$.actividad").doesNotExist())
                .andExpect(jsonPath("$.zona").doesNotExist())
                .andExpect(jsonPath("$.perfilPrestador").doesNotExist());
    }

    @Test
    @DisplayName("una oferta inexistente o no visible responde 404 OFERTA_NO_ENCONTRADA en formato ProblemDetail")
    void fichaNoEncontrada() throws Exception {
        when(consultarFicha.consultar("no-existe")).thenThrow(new OfertaNoEncontradaException("no-existe"));

        http.perform(get("/ofertas/no-existe"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.codigo").value("OFERTA_NO_ENCONTRADA"));
    }
}
