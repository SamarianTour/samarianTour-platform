package co.edu.unimagdalena.samariantour.catalogo.catalogo.infraestructura.entrada.rest;

import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.excepcion.OfertaNoEncontradaException;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.Oferta;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.Pagina;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.Paginacion;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.TipoPrestador;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.puerto.entrada.ListarPendientesModeracionUseCase;
import co.edu.unimagdalena.samariantour.catalogo.compartido.seguridad.IdentidadPorHeaders;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.util.List;

import static co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.DatosDePrueba.AHORA;
import static co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.DatosDePrueba.DESPUES;
import static co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.DatosDePrueba.PRESTADOR;
import static co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.DatosDePrueba.actividad;
import static co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.DatosDePrueba.habilitado;
import static co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.DatosDePrueba.ofertaEnModeracion;
import static co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.DatosDePrueba.ofertaPublicada;
import static co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.DatosDePrueba.paquete;
import static co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.DatosDePrueba.zonaActiva;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ModeracionController.class)
@Import(IdentidadPorHeaders.class)
@ActiveProfiles("demo")
class ModeracionControllerTest {

    private static final String ADMINISTRADOR = "60000000-0000-0000-0000-000000000006";

    @Autowired
    private MockMvc http;

    @MockitoBean
    private ListarPendientesModeracionUseCase pendientes;

    @Test
    @DisplayName("el administrador ve la versión en moderación y, si es una edición, la publicada para comparar")
    void ofertasPendientes() throws Exception {
        Oferta editada = ofertaPublicada();
        editada.editar(habilitado(), TipoPrestador.AGENCIA_OPERADORA, actividad("Caminata al amanecer"),
                zonaActiva(), DESPUES);
        when(pendientes.listarOfertas(new Paginacion(0, 10)))
                .thenReturn(new Pagina<>(List.of(editada), new Paginacion(0, 10), 1));

        http.perform(comoAdministrador(get("/admin/moderacion/ofertas").param("page", "0").param("size", "10")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contenido", hasSize(1)))
                .andExpect(jsonPath("$.contenido[0].id").value("oferta-1"))
                .andExpect(jsonPath("$.contenido[0].tipo").value("ACTIVIDAD"))
                .andExpect(jsonPath("$.contenido[0].prestadorId").value(PRESTADOR.toString()))
                .andExpect(jsonPath("$.contenido[0].rntPrestadorActivo").value(true))
                .andExpect(jsonPath("$.contenido[0].numeroVersion").value(2))
                .andExpect(jsonPath("$.contenido[0].enviadaEn").value(DESPUES.toString()))
                .andExpect(jsonPath("$.contenido[0].pendiente.nombre").value("Caminata al amanecer"))
                .andExpect(jsonPath("$.contenido[0].pendiente.unidadPrecio").value("por persona"))
                .andExpect(jsonPath("$.contenido[0].pendiente.tipoActividad").value("SENDERISMO"))
                .andExpect(jsonPath("$.contenido[0].pendiente.horariosSalida[0]").value("07:00"))
                .andExpect(jsonPath("$.contenido[0].pendiente.accesibleSillaRuedas").value(false))
                .andExpect(jsonPath("$.contenido[0].pendiente.ubicacion.latitud").value(11.31))
                .andExpect(jsonPath("$.contenido[0].pendiente.actividadIds").doesNotExist())
                .andExpect(jsonPath("$.contenido[0].pendiente.tipoUnidad").doesNotExist())
                .andExpect(jsonPath("$.contenido[0].publicada.nombre").value("Caminata a Cabo San Juan"))
                .andExpect(jsonPath("$.totalElementos").value(1));
    }

    @Test
    @DisplayName("una oferta nueva no tiene versión publicada, y un paquete no lleva accesibilidad propia")
    void paqueteNuevo() throws Exception {
        Oferta caminata = ofertaPublicada();
        Oferta otra = Oferta.publicar("oferta-2", habilitado(), TipoPrestador.AGENCIA_OPERADORA,
                actividad("Kayak"), zonaActiva(), AHORA);
        Oferta nuevo = Oferta.publicar("paquete-1", habilitado(), TipoPrestador.AGENCIA_OPERADORA,
                paquete("oferta-1", "oferta-2"), zonaActiva(), List.of(caminata, otra), AHORA);
        when(pendientes.listarOfertas(Paginacion.de(null, null)))
                .thenReturn(new Pagina<>(List.of(nuevo), Paginacion.de(null, null), 1));

        http.perform(comoAdministrador(get("/admin/moderacion/ofertas")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contenido[0].tipo").value("PAQUETE"))
                .andExpect(jsonPath("$.contenido[0].numeroVersion").value(1))
                .andExpect(jsonPath("$.contenido[0].publicada").doesNotExist())
                .andExpect(jsonPath("$.contenido[0].pendiente.actividadIds[1]").value("oferta-2"))
                .andExpect(jsonPath("$.contenido[0].pendiente.duracionMinutos").value(600))
                .andExpect(jsonPath("$.contenido[0].pendiente.accesibleSillaRuedas").doesNotExist());
    }

    @Test
    @DisplayName("la lista es solo para el administrador: sin identidad 401 y con otro rol 403 (DC-13)")
    void soloAdministrador() throws Exception {
        http.perform(get("/admin/moderacion/ofertas"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.codigo").value("NO_AUTENTICADO"));
        http.perform(get("/admin/moderacion/ofertas")
                        .header("X-Usuario-Id", PRESTADOR.toString()).header("X-Rol", "PRESTADOR")
                        .header("X-Tipo-Prestador", "AGENCIA_OPERADORA"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.codigo").value("ACCESO_DENEGADO"));

        verifyNoInteractions(pendientes);
    }

    @Test
    @DisplayName("el núcleo consulta una oferta en moderación sin identidad de usuario: la ruta es interna")
    void consultaDelNucleo() throws Exception {
        when(pendientes.consultarOferta("oferta-1")).thenReturn(ofertaEnModeracion());

        http.perform(get("/internal/moderacion/ofertas/oferta-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("oferta-1"))
                .andExpect(jsonPath("$.tipo").value("ACTIVIDAD"))
                .andExpect(jsonPath("$.nombre").value("Caminata a Cabo San Juan"))
                .andExpect(jsonPath("$.prestadorId").value(PRESTADOR.toString()))
                .andExpect(jsonPath("$.numeroVersion").value(1))
                .andExpect(jsonPath("$.enviadaEn").value(AHORA.toString()));
        verify(pendientes).consultarOferta("oferta-1");
    }

    @Test
    @DisplayName("una oferta que no está en moderación responde 404 OFERTA_NO_ENCONTRADA")
    void ofertaFueraDeModeracion() throws Exception {
        when(pendientes.consultarOferta("oferta-1")).thenThrow(new OfertaNoEncontradaException("oferta-1"));

        http.perform(get("/internal/moderacion/ofertas/oferta-1"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.codigo").value("OFERTA_NO_ENCONTRADA"));
    }

    private static MockHttpServletRequestBuilder comoAdministrador(MockHttpServletRequestBuilder peticion) {
        return peticion.header("X-Usuario-Id", ADMINISTRADOR).header("X-Rol", "ADMINISTRADOR");
    }
}
