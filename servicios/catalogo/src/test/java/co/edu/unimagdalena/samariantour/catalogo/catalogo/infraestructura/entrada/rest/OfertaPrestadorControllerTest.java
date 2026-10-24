package co.edu.unimagdalena.samariantour.catalogo.catalogo.infraestructura.entrada.rest;

import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.excepcion.OfertaNoEditableException;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.excepcion.OfertaNoEncontradaException;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.excepcion.PrestadorNoAutorizadoException;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.excepcion.RntInactivoException;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.excepcion.TipoOfertaNoPermitidoException;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.excepcion.ZonaNoDisponibleException;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.Foto;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.NivelEsfuerzo;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.Oferta;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.Pagina;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.Paginacion;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.TipoOferta;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.TipoPrestador;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.VersionActividad;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.VersionPaquete;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.puerto.entrada.GestionarOfertaUseCase;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.puerto.entrada.comando.EditarOfertaCommand;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.puerto.entrada.comando.PublicarOfertaCommand;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.puerto.entrada.comando.RetirarOfertaCommand;
import co.edu.unimagdalena.samariantour.catalogo.compartido.concurrencia.ConflictoConcurrenciaException;
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
import java.util.List;

import static co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.DatosDePrueba.DESPUES;
import static co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.DatosDePrueba.FOTO;
import static co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.DatosDePrueba.PRESTADOR;
import static co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.DatosDePrueba.ZONA_ACTIVA;
import static co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.DatosDePrueba.actividad;
import static co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.DatosDePrueba.habilitado;
import static co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.DatosDePrueba.ofertaEnModeracion;
import static co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.DatosDePrueba.ofertaPublicada;
import static co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.DatosDePrueba.zonaActiva;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
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
              "fotos": ["3f2504e0-4f89-11d3-9a0c-0305e82c3302", "3f2504e0-4f89-11d3-9a0c-0305e82c3301"],
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
                .andExpect(jsonPath("$.pendiente.revisadaEn").doesNotExist())
                .andExpect(jsonPath("$.pendiente.portada.urlMiniatura")
                        .value("/fotos/" + FOTO.id() + "/miniatura"));

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
            assertThat(actividad.contenido().fotos()).extracting(Foto::id).containsExactly(
                    "3f2504e0-4f89-11d3-9a0c-0305e82c3302", "3f2504e0-4f89-11d3-9a0c-0305e82c3301");
        });
    }

    @Test
    @DisplayName("una foto con un identificador mal formado responde 400 en el campo fotos, sin llamar al caso de uso")
    void fotoMalFormada() throws Exception {
        http.perform(comoAgencia(post("/prestador/ofertas"))
                        .content(ACTIVIDAD.replace("3f2504e0-4f89-11d3-9a0c-0305e82c3302", "../secreto")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("DATO_INVALIDO"))
                .andExpect(jsonPath("$.detail").value("fotos: tiene un identificador que no es de una foto"));
        verifyNoInteractions(gestionarOferta);
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
                        .header("X-Usuario-Id", "no-es-un-uuid").header("X-Rol", "PRESTADOR")
                        .header("X-Tipo-Prestador", "AGENCIA_OPERADORA"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.codigo").value("NO_AUTENTICADO"));
        http.perform(post("/prestador/ofertas").contentType(MediaType.APPLICATION_JSON).content(ACTIVIDAD)
                        .header("X-Usuario-Id", PRESTADOR.toString()).header("X-Tipo-Prestador", "AGENCIA_OPERADORA"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail", containsString("X-Rol")));

        verifyNoInteractions(gestionarOferta);
    }

    @Test
    @DisplayName("un usuario con otro rol no entra a las rutas del prestador: 403 ACCESO_DENEGADO")
    void otroRol() throws Exception {
        for (String rol : new String[] {"TURISTA", "ADMINISTRADOR"}) {
            http.perform(get("/prestador/ofertas").header("X-Usuario-Id", PRESTADOR.toString()).header("X-Rol", rol))
                    .andExpect(status().isForbidden())
                    .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                    .andExpect(jsonPath("$.codigo").value("ACCESO_DENEGADO"));
        }

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
                        .header("X-Usuario-Id", PRESTADOR.toString()).header("X-Rol", "PRESTADOR")
                        .header("X-Tipo-Prestador", "HOTEL"))
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

        // Los reintentos del servidor no alcanzaron: el cliente puede volver a intentarlo (ADR-05).
        doThrow(new ConflictoConcurrenciaException("cruce")).when(gestionarOferta).editar(any());
        http.perform(comoAgencia(put("/prestador/ofertas/oferta-1")).content(ACTIVIDAD))
                .andExpect(status().isConflict())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.codigo").value("CONFLICTO_CONCURRENCIA"));
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

    @Test
    @DisplayName("retirar responde 204 sin cuerpo y entrega al caso de uso el id de la ruta y el prestador")
    void retirar() throws Exception {
        http.perform(comoAgencia(patch("/prestador/ofertas/oferta-1/retiro")))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        ArgumentCaptor<RetirarOfertaCommand> comando = ArgumentCaptor.forClass(RetirarOfertaCommand.class);
        verify(gestionarOferta).retirar(comando.capture());
        assertThat(comando.getValue().ofertaId()).isEqualTo("oferta-1");
        assertThat(comando.getValue().prestadorId()).isEqualTo(PRESTADOR);
    }

    @Test
    @DisplayName("retirar traduce sus errores: 404 si no existe, 403 si es de otro y 409 si los reintentos no alcanzan")
    void erroresAlRetirar() throws Exception {
        doThrow(new OfertaNoEncontradaException("oferta-1")).when(gestionarOferta).retirar(any());
        http.perform(comoAgencia(patch("/prestador/ofertas/oferta-1/retiro")))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.codigo").value("OFERTA_NO_ENCONTRADA"));

        doThrow(new PrestadorNoAutorizadoException("oferta-1")).when(gestionarOferta).retirar(any());
        http.perform(comoAgencia(patch("/prestador/ofertas/oferta-1/retiro")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.codigo").value("PRESTADOR_NO_AUTORIZADO"));

        doThrow(new ConflictoConcurrenciaException("cruce")).when(gestionarOferta).retirar(any());
        http.perform(comoAgencia(patch("/prestador/ofertas/oferta-1/retiro")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("CONFLICTO_CONCURRENCIA"));
    }

    @Test
    @DisplayName("listar responde la página de ofertas propias, con la retirada marcada, y pide la página indicada")
    void listarMias() throws Exception {
        Oferta retirada = ofertaPublicada();
        retirada.retirar(PRESTADOR);
        when(gestionarOferta.listarMias(eq(PRESTADOR), any()))
                .thenReturn(new Pagina<>(List.of(ofertaEnModeracion(), retirada), new Paginacion(1, 2), 4));

        http.perform(comoAgencia(get("/prestador/ofertas")).param("page", "1").param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contenido", hasSize(2)))
                .andExpect(jsonPath("$.contenido[0].retirada").value(false))
                .andExpect(jsonPath("$.contenido[0].publicada").doesNotExist())
                .andExpect(jsonPath("$.contenido[0].pendiente.estadoModeracion").value("EN_MODERACION"))
                .andExpect(jsonPath("$.contenido[0].pendiente.unidadPrecio").value("por persona"))
                .andExpect(jsonPath("$.contenido[1].retirada").value(true))
                .andExpect(jsonPath("$.contenido[1].publicada.numero").value(1))
                .andExpect(jsonPath("$.pagina").value(1))
                .andExpect(jsonPath("$.tamano").value(2))
                .andExpect(jsonPath("$.totalElementos").value(4))
                .andExpect(jsonPath("$.totalPaginas").value(2));

        verify(gestionarOferta).listarMias(PRESTADOR, new Paginacion(1, 2));
    }

    @Test
    @DisplayName("consultar una oferta propia trae el contenido completo de la versión publicada y de la pendiente")
    void consultarMia() throws Exception {
        Oferta editada = ofertaPublicada();
        editada.editar(habilitado(), TipoPrestador.AGENCIA_OPERADORA, actividad("Caminata al amanecer"),
                zonaActiva(), DESPUES);
        when(gestionarOferta.consultarMia("oferta-1", PRESTADOR)).thenReturn(editada);

        http.perform(comoAgencia(get("/prestador/ofertas/oferta-1")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("oferta-1"))
                .andExpect(jsonPath("$.retirada").value(false))
                .andExpect(jsonPath("$.publicada.numero").value(1))
                .andExpect(jsonPath("$.publicada.estadoModeracion").value("APROBADA"))
                .andExpect(jsonPath("$.publicada.contenido.nombre").value("Caminata a Cabo San Juan"))
                .andExpect(jsonPath("$.publicada.contenido.tipoActividad").value("SENDERISMO"))
                .andExpect(jsonPath("$.publicada.contenido.horariosSalida[0]").value("07:00"))
                .andExpect(jsonPath("$.publicada.contenido.unidadPrecio").value("por persona"))
                .andExpect(jsonPath("$.publicada.contenido.fotos[0].url").exists())
                .andExpect(jsonPath("$.pendiente.numero").value(2))
                .andExpect(jsonPath("$.pendiente.estadoModeracion").value("EN_MODERACION"))
                .andExpect(jsonPath("$.pendiente.revisadaEn").doesNotExist())
                .andExpect(jsonPath("$.pendiente.contenido.nombre").value("Caminata al amanecer"));
    }

    @Test
    @DisplayName("una oferta rechazada que nunca se publicó trae su versión pendiente con el motivo, para corregirla")
    void consultarRechazada() throws Exception {
        Oferta rechazada = ofertaEnModeracion();
        rechazada.rechazar(1, "Faltan los horarios de salida", DESPUES);
        when(gestionarOferta.consultarMia("oferta-1", PRESTADOR)).thenReturn(rechazada);

        http.perform(comoAgencia(get("/prestador/ofertas/oferta-1")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.publicada").doesNotExist())
                .andExpect(jsonPath("$.pendiente.estadoModeracion").value("RECHAZADA"))
                .andExpect(jsonPath("$.pendiente.motivoRechazo").value("Faltan los horarios de salida"))
                .andExpect(jsonPath("$.pendiente.revisadaEn").value(DESPUES.toString()))
                .andExpect(jsonPath("$.pendiente.contenido.descripcion").exists());
    }

    @Test
    @DisplayName("consultar traduce sus errores como editar y retirar: 404 si no existe y 403 si es de otro prestador")
    void erroresAlConsultar() throws Exception {
        when(gestionarOferta.consultarMia("no-existe", PRESTADOR))
                .thenThrow(new OfertaNoEncontradaException("no-existe"));
        when(gestionarOferta.consultarMia("ajena", PRESTADOR)).thenThrow(new PrestadorNoAutorizadoException("ajena"));

        http.perform(comoAgencia(get("/prestador/ofertas/no-existe")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.codigo").value("OFERTA_NO_ENCONTRADA"));
        http.perform(comoAgencia(get("/prestador/ofertas/ajena")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.codigo").value("PRESTADOR_NO_AUTORIZADO"));
        http.perform(get("/prestador/ofertas/oferta-1"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.codigo").value("NO_AUTENTICADO"));
    }

    @Test
    @DisplayName("listar sin parámetros pide la primera página con el tamaño por defecto")
    void listarPaginaPorDefecto() throws Exception {
        when(gestionarOferta.listarMias(any(), any()))
                .thenReturn(new Pagina<>(List.of(), Paginacion.de(null, null), 0));

        http.perform(comoAgencia(get("/prestador/ofertas")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contenido", hasSize(0)))
                .andExpect(jsonPath("$.totalPaginas").value(0));

        verify(gestionarOferta).listarMias(PRESTADOR, new Paginacion(0, Paginacion.TAMANO_POR_DEFECTO));
    }

    @Test
    @DisplayName("listar con una paginación inválida responde 400 DATO_INVALIDO y no llama al caso de uso")
    void listarPaginacionInvalida() throws Exception {
        http.perform(comoAgencia(get("/prestador/ofertas")).param("size", "101"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("DATO_INVALIDO"))
                .andExpect(jsonPath("$.detail", containsString("size")));
        http.perform(comoAgencia(get("/prestador/ofertas")).param("page", "uno"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("DATO_INVALIDO"));

        verifyNoInteractions(gestionarOferta);
    }

    @Test
    @DisplayName("retirar y listar también exigen la identidad del prestador: 401 NO_AUTENTICADO")
    void retirarYListarSinIdentidad() throws Exception {
        http.perform(patch("/prestador/ofertas/oferta-1/retiro"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.codigo").value("NO_AUTENTICADO"));
        http.perform(get("/prestador/ofertas"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.codigo").value("NO_AUTENTICADO"));

        verifyNoInteractions(gestionarOferta);
    }

    private static MockHttpServletRequestBuilder comoAgencia(MockHttpServletRequestBuilder peticion) {
        return peticion.contentType(MediaType.APPLICATION_JSON)
                .header("X-Usuario-Id", PRESTADOR.toString())
                .header("X-Rol", "PRESTADOR")
                .header("X-Tipo-Prestador", "AGENCIA_OPERADORA");
    }
}
