package co.edu.unimagdalena.samariantour.catalogo;

import co.edu.unimagdalena.samariantour.catalogo.catalogo.infraestructura.salida.almacenamiento.ImagenesDePrueba;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * El servicio completo con los perfiles de la demostración: controladores, casos de uso y adaptadores en memoria
 * con los datos de Santa Marta. No necesita MongoDB ni Docker.
 */
@SpringBootTest(properties = {"spring.autoconfigure.exclude="
        + "org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration,"
        + "org.springframework.boot.autoconfigure.data.mongo.MongoDataAutoConfiguration",
        // Las fotos van a una carpeta del build, no a la carpeta temporal del equipo.
        "catalogo.fotos.carpeta=target/fotos-demostracion"})
@AutoConfigureMockMvc
@ActiveProfiles({"memoria", "demo"})
class DemostracionApiTest {

    private static final String GUIA = "20000000-0000-0000-0000-000000000002";

    /** Una actividad del guía con una foto; {@code %s} es el identificador de la foto, que se sube antes. */
    private static final String NUEVA_ACTIVIDAD = """
            {
              "tipo": "ACTIVIDAD",
              "nombre": "Caminata nocturna en Minca",
              "descripcion": "Recorrido para observar ranas e insectos al caer la noche",
              "precioReferencia": 75000,
              "zonaId": "a00000000000000000000007",
              "capacidadMaxima": 6,
              "fotos": ["%s"],
              "tipoActividad": "AVISTAMIENTO_FAUNA",
              "nivelEsfuerzo": "BAJO",
              "horariosSalida": ["18:30"],
              "puntoEncuentro": "Plaza principal de Minca",
              "duracionMinutos": 120,
              "modalidad": "CAMINATA",
              "idiomasAtencion": ["es", "en"]
            }
            """;

    @Autowired
    private MockMvc http;

    @Test
    @DisplayName("[Demostración] buscar → ficha → publicar → no aparece → aprobar con un aviso → aparece → editar "
            + "→ sigue la versión aprobada")
    void flujoDeLaDemostracion() throws Exception {
        http.perform(get("/ofertas").param("tipo", "ACTIVIDAD").param("zonaId", "a00000000000000000000005"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contenido[*].id", hasItem("b00000000000000000000001")));
        http.perform(get("/ofertas/b00000000000000000000008"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paquete.actividades", hasSize(2)))
                .andExpect(jsonPath("$.paquete.nivelEsfuerzo").value("MODERADO"))
                .andExpect(jsonPath("$.zona.areaProtegida").value(true))
                .andExpect(jsonPath("$.ubicacion.latitud").value(11.2785))
                .andExpect(jsonPath("$.ubicacion.longitud").value(-73.9255))
                .andExpect(jsonPath("$.perfilPrestador.nombreComercial").value("Mar y Selva Expediciones"))
                .andExpect(jsonPath("$.perfilPrestador.descripcion").exists())
                .andExpect(jsonPath("$.fotos", hasSize(1)));

        String fotoId = subirFoto(GUIA, "GUIA_INDEPENDIENTE");
        String creada = http.perform(post("/prestador/ofertas").contentType(MediaType.APPLICATION_JSON)
                        .header("X-Usuario-Id", GUIA).header("X-Rol", "PRESTADOR")
                        .header("X-Tipo-Prestador", "GUIA_INDEPENDIENTE")
                        .content(NUEVA_ACTIVIDAD.formatted(fotoId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.pendiente.numero").value(1))
                .andExpect(jsonPath("$.pendiente.portada.id").value(fotoId))
                .andExpect(jsonPath("$.pendiente.estadoModeracion").value("EN_MODERACION"))
                .andReturn().getResponse().getContentAsString();
        String id = JsonPath.read(creada, "$.id");

        http.perform(get("/ofertas/" + id)).andExpect(status().isNotFound())
                .andExpect(jsonPath("$.codigo").value("OFERTA_NO_ENCONTRADA"));
        http.perform(get("/ofertas").param("zonaId", "a00000000000000000000007"))
                .andExpect(jsonPath("$.contenido[*].id", not(hasItem(id))));

        String aviso = """
                {"idAviso": "demo-1", "tipo": "MODERACION", "ofertaId": "%s", "numeroVersion": 1, "decision": "APROBAR"}
                """.formatted(id);
        http.perform(post("/internal/avisos").contentType(MediaType.APPLICATION_JSON).content(aviso))
                .andExpect(status().isOk()).andExpect(jsonPath("$.resultado").value("APLICADO"));
        http.perform(post("/internal/avisos").contentType(MediaType.APPLICATION_JSON).content(aviso))
                .andExpect(status().isOk()).andExpect(jsonPath("$.resultado").value("DUPLICADO"));

        http.perform(get("/ofertas").param("zonaId", "a00000000000000000000007"))
                .andExpect(jsonPath("$.contenido[*].id", hasItem(id)))
                .andExpect(jsonPath("$.contenido[?(@.id == '%s')].portada.urlMiniatura".formatted(id),
                        contains("/fotos/" + fotoId + "/miniatura")));
        http.perform(get("/ofertas/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.precioReferencia").value(75000))
                .andExpect(jsonPath("$.actividad.horariosSalida[0]").value("18:30"))
                .andExpect(jsonPath("$.fotos[0].url").value("/fotos/" + fotoId + "/galeria"));

        http.perform(put("/prestador/ofertas/" + id).contentType(MediaType.APPLICATION_JSON)
                        .header("X-Usuario-Id", GUIA).header("X-Rol", "PRESTADOR")
                        .header("X-Tipo-Prestador", "GUIA_INDEPENDIENTE")
                        .content(NUEVA_ACTIVIDAD.formatted(fotoId).replace("75000", "82000")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.publicada.numero").value(1))
                .andExpect(jsonPath("$.pendiente.numero").value(2));
        http.perform(get("/ofertas/" + id)).andExpect(jsonPath("$.precioReferencia").value(75000));
    }

    @Test
    @DisplayName("[Demostración] retirar una oferta aprobada: sale de la búsqueda, sigue en la lista del prestador "
            + "como retirada y ya no se puede editar")
    void retiro() throws Exception {
        String fotoId = subirFoto(GUIA, "GUIA_INDEPENDIENTE");
        String creada = http.perform(post("/prestador/ofertas").contentType(MediaType.APPLICATION_JSON)
                        .header("X-Usuario-Id", GUIA).header("X-Rol", "PRESTADOR")
                        .header("X-Tipo-Prestador", "GUIA_INDEPENDIENTE")
                        .content(NUEVA_ACTIVIDAD.formatted(fotoId)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String id = JsonPath.read(creada, "$.id");
        http.perform(post("/internal/avisos").contentType(MediaType.APPLICATION_JSON).content("""
                        {"idAviso": "retiro-1", "tipo": "MODERACION", "ofertaId": "%s", "numeroVersion": 1,
                         "decision": "APROBAR"}
                        """.formatted(id)))
                .andExpect(jsonPath("$.resultado").value("APLICADO"));
        http.perform(get("/ofertas/" + id)).andExpect(status().isOk());

        http.perform(patch("/prestador/ofertas/" + id + "/retiro")
                        .header("X-Usuario-Id", "10000000-0000-0000-0000-000000000001").header("X-Rol", "PRESTADOR")
                        .header("X-Tipo-Prestador", "AGENCIA_OPERADORA"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.codigo").value("PRESTADOR_NO_AUTORIZADO"));
        http.perform(patch("/prestador/ofertas/" + id + "/retiro")
                        .header("X-Usuario-Id", GUIA).header("X-Rol", "PRESTADOR")
                        .header("X-Tipo-Prestador", "GUIA_INDEPENDIENTE"))
                .andExpect(status().isNoContent());

        http.perform(get("/ofertas/" + id)).andExpect(status().isNotFound());
        http.perform(get("/ofertas").param("zonaId", "a00000000000000000000007").param("size", "100"))
                .andExpect(jsonPath("$.contenido[*].id", not(hasItem(id))));
        http.perform(get("/prestador/ofertas").param("size", "100")
                        .header("X-Usuario-Id", GUIA).header("X-Rol", "PRESTADOR")
                        .header("X-Tipo-Prestador", "GUIA_INDEPENDIENTE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contenido[?(@.id == '%s')].retirada".formatted(id), contains(true)));
        http.perform(put("/prestador/ofertas/" + id).contentType(MediaType.APPLICATION_JSON)
                        .header("X-Usuario-Id", GUIA).header("X-Rol", "PRESTADOR")
                        .header("X-Tipo-Prestador", "GUIA_INDEPENDIENTE")
                        .content(NUEVA_ACTIVIDAD.formatted(fotoId)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("OFERTA_NO_EDITABLE"));
    }

    @Test
    @DisplayName("un prestador con el RNT inactivo no puede publicar: 403 RNT_INACTIVO")
    void prestadorConRntInactivo() throws Exception {
        String sinRnt = "50000000-0000-0000-0000-000000000005";
        // Subir una foto no publica nada, así que no exige el RNT; publicar sí.
        String fotoId = subirFoto(sinRnt, "AGENCIA_OPERADORA");
        http.perform(post("/prestador/ofertas").contentType(MediaType.APPLICATION_JSON)
                        .header("X-Usuario-Id", sinRnt).header("X-Rol", "PRESTADOR")
                        .header("X-Tipo-Prestador", "AGENCIA_OPERADORA")
                        .content(NUEVA_ACTIVIDAD.formatted(fotoId)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.codigo").value("RNT_INACTIVO"));
    }

    @Test
    @DisplayName("el núcleo consulta una oferta visible con su versión aprobada; una sin aprobar o sin RNT da 404")
    void consultaInternaDelNucleo() throws Exception {
        http.perform(get("/internal/ofertas/b00000000000000000000001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Caminata a Cabo San Juan del Guía"))
                .andExpect(jsonPath("$.prestadorId").value("10000000-0000-0000-0000-000000000001"))
                .andExpect(jsonPath("$.zonaId").value("a00000000000000000000005"))
                .andExpect(jsonPath("$.horariosSalida", contains("07:00", "08:00")))
                .andExpect(jsonPath("$.capacidadMaxima").value(12));
        http.perform(get("/internal/ofertas/b00000000000000000000013"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tipo").value("RESTAURANTE"))
                .andExpect(jsonPath("$.horariosSalida").doesNotExist());
        http.perform(get("/internal/ofertas/b00000000000000000000017"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.codigo").value("OFERTA_NO_ENCONTRADA"));
        http.perform(get("/internal/ofertas/b00000000000000000000016"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("la recomendación lee las mismas ofertas que la búsqueda, con la alta afluencia de su zona (RF-29)")
    void ofertasVisiblesParaLaRecomendacion() throws Exception {
        String busqueda = http.perform(get("/ofertas").param("size", "100"))
                .andReturn().getResponse().getContentAsString();
        int visibles = JsonPath.read(busqueda, "$.totalElementos");
        http.perform(get("/internal/ofertas").param("size", "100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElementos").value(visibles))
                .andExpect(jsonPath("$.contenido[*].id", not(hasItem("b00000000000000000000016"))));

        http.perform(get("/internal/ofertas").param("zonaId", "a00000000000000000000005").param("tipo", "PAQUETE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contenido", hasSize(1)))
                .andExpect(jsonPath("$.contenido[0].id").value("b00000000000000000000008"))
                .andExpect(jsonPath("$.contenido[0].zonaAltaAfluencia").value(true))
                .andExpect(jsonPath("$.contenido[0].tiposActividad", contains("SENDERISMO", "BUCEO_SNORKEL")))
                .andExpect(jsonPath("$.contenido[0].nivelEsfuerzo").value("MODERADO"));
        http.perform(get("/internal/ofertas").param("zonaId", "a00000000000000000000001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contenido[*].zonaAltaAfluencia", not(hasItem(true))));
    }

    @Test
    @DisplayName("el administrador ve las ofertas en moderación de la precarga, primero la que lleva más tiempo, y el "
            + "núcleo consulta la que va a decidir")
    void moderacion() throws Exception {
        http.perform(get("/admin/moderacion/ofertas")
                        .header("X-Usuario-Id", "60000000-0000-0000-0000-000000000006")
                        .header("X-Rol", "ADMINISTRADOR"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contenido[0].id").value("b00000000000000000000017"))
                .andExpect(jsonPath("$.contenido[0].publicada").doesNotExist())
                .andExpect(jsonPath("$.contenido[1].id").value("b00000000000000000000001"))
                .andExpect(jsonPath("$.contenido[1].numeroVersion").value(2))
                .andExpect(jsonPath("$.contenido[1].pendiente.precioReferencia").value(165000))
                .andExpect(jsonPath("$.contenido[1].publicada.precioReferencia").value(150000));

        http.perform(get("/internal/moderacion/ofertas/b00000000000000000000017"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Paseo en lancha a Playa Blanca"))
                .andExpect(jsonPath("$.prestadorId").value("10000000-0000-0000-0000-000000000001"))
                .andExpect(jsonPath("$.numeroVersion").value(1));
        http.perform(get("/internal/moderacion/ofertas/b00000000000000000000013"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("[Demostración] opiniones: el núcleo habilita al turista, el turista opina, el administrador la ve en "
            + "moderación, el núcleo la aprueba, aparece en la ficha y la supresión del turista la borra")
    void opiniones() throws Exception {
        String turista = "75000000-0000-0000-0000-000000000001";
        String oferta = "b00000000000000000000006";
        String opinion = """
                {"ofertaId": "%s", "calificacion": 4, "comentario": "Recorrido muy completo por el centro histórico."}
                """.formatted(oferta);

        http.perform(post("/turista/opiniones").contentType(MediaType.APPLICATION_JSON)
                        .header("X-Usuario-Id", turista).header("X-Rol", "TURISTA").content(opinion))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.codigo").value("OPINION_NO_HABILITADA"));

        http.perform(post("/internal/opiniones/avisos").contentType(MediaType.APPLICATION_JSON).content("""
                        {"idAviso": "pago-1", "tipo": "HABILITACION_OPINION", "turistaId": "%s", "ofertaId": "%s"}
                        """.formatted(turista, oferta)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.resultado").value("APLICADO"));

        String creada = http.perform(post("/turista/opiniones").contentType(MediaType.APPLICATION_JSON)
                        .header("X-Usuario-Id", turista).header("X-Rol", "TURISTA").content(opinion))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.estado").value("EN_MODERACION"))
                .andExpect(jsonPath("$.nombreOferta").value("Recorrido a pie por el Centro Histórico"))
                .andReturn().getResponse().getContentAsString();
        String id = JsonPath.read(creada, "$.id");
        http.perform(post("/turista/opiniones").contentType(MediaType.APPLICATION_JSON)
                        .header("X-Usuario-Id", turista).header("X-Rol", "TURISTA").content(opinion))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("OPINION_DUPLICADA"));

        http.perform(get("/ofertas/" + oferta + "/opiniones"))
                .andExpect(jsonPath("$.contenido[*].id", not(hasItem(id))));
        http.perform(get("/admin/moderacion/opiniones").param("size", "100")
                        .header("X-Usuario-Id", "60000000-0000-0000-0000-000000000006")
                        .header("X-Rol", "ADMINISTRADOR"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contenido[*].id", hasItem(id)));
        http.perform(get("/internal/moderacion/opiniones/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.turistaId").value(turista));

        String aprobar = """
                {"idAviso": "moderacion-1", "tipo": "MODERACION_OPINION", "opinionId": "%s", "decision": "APROBAR"}
                """.formatted(id);
        http.perform(post("/internal/opiniones/avisos").contentType(MediaType.APPLICATION_JSON).content(aprobar))
                .andExpect(jsonPath("$.resultado").value("APLICADO"));
        http.perform(post("/internal/opiniones/avisos").contentType(MediaType.APPLICATION_JSON).content(aprobar))
                .andExpect(jsonPath("$.resultado").value("DUPLICADO"));

        http.perform(get("/ofertas/" + oferta + "/opiniones"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contenido[0].id").value(id))
                .andExpect(jsonPath("$.resumen.totalOpiniones").value(1))
                .andExpect(jsonPath("$.resumen.promedioCalificacion").value(4.0));
        http.perform(get("/turista/opiniones").header("X-Usuario-Id", turista).header("X-Rol", "TURISTA"))
                .andExpect(jsonPath("$.contenido[0].estado").value("APROBADA"));

        http.perform(post("/internal/opiniones/avisos").contentType(MediaType.APPLICATION_JSON).content("""
                        {"idAviso": "supresion-1", "tipo": "SUPRESION_TURISTA", "turistaId": "%s"}
                        """.formatted(turista)))
                .andExpect(jsonPath("$.resultado").value("APLICADO"));
        http.perform(get("/ofertas/" + oferta + "/opiniones"))
                .andExpect(jsonPath("$.resumen.totalOpiniones").value(0));
        http.perform(get("/turista/opiniones").header("X-Usuario-Id", turista).header("X-Rol", "TURISTA"))
                .andExpect(jsonPath("$.totalElementos").value(0));
    }

    @Test
    @DisplayName("las opiniones de la precarga: 3 aprobadas en la caminata a Cabo San Juan, sin la rechazada")
    void opinionesDeLaPrecarga() throws Exception {
        http.perform(get("/ofertas/b00000000000000000000001/opiniones"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resumen.totalOpiniones").value(3))
                .andExpect(jsonPath("$.resumen.promedioCalificacion").value(4.7))
                .andExpect(jsonPath("$.contenido", hasSize(3)));
        http.perform(get("/ofertas/b00000000000000000000016/opiniones"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.codigo").value("OFERTA_NO_ENCONTRADA"));
    }

    @Test
    @DisplayName("[Demostración] el guía completa su perfil, sube una foto de celular, se ve en la ficha de su oferta "
            + "y la quita")
    void perfilConFotos() throws Exception {
        http.perform(comoGuia(put("/prestador/perfil")).contentType(MediaType.APPLICATION_JSON).content("""
                        {"nombreComercial": "Aves de Minca",
                         "descripcion": "Guía independiente de Minca, especializado en aves y cultura cafetera",
                         "horariosAtencion": "Todos los días desde las 5:30"}
                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombreComercial").value("Aves de Minca"));

        // Una foto de celular: acostada, con la orientación en sus metadatos EXIF.
        String perfil = http.perform(comoGuia(multipart("/prestador/perfil/fotos").file(new MockMultipartFile(
                        "foto", "IMG_2024.jpg", "image/jpeg", ImagenesDePrueba.jpgConOrientacion(1600, 1200, 6)))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.fotos", hasSize(1)))
                .andReturn().getResponse().getContentAsString();
        String fotoId = JsonPath.read(perfil, "$.fotos[0].id");
        String url = JsonPath.read(perfil, "$.fotos[0].url");

        byte[] galeria = http.perform(get(url))
                .andExpect(status().isOk())
                .andExpect(content().contentType("image/jpeg"))
                .andReturn().getResponse().getContentAsByteArray();
        assertThat(ImagenesDePrueba.leer(galeria).getHeight()).as("enderezada: queda vertical").isEqualTo(1280);
        http.perform(get("/ofertas/b0000000000000000000000a"))
                .andExpect(jsonPath("$.perfilPrestador.nombreComercial").value("Aves de Minca"))
                .andExpect(jsonPath("$.perfilPrestador.fotos[0].urlMiniatura")
                        .value("/fotos/" + fotoId + "/miniatura"));

        http.perform(comoGuia(delete("/prestador/perfil/fotos/" + fotoId))).andExpect(status().isNoContent());
        http.perform(get(url)).andExpect(status().isNotFound());
        http.perform(comoGuia(get("/prestador/perfil"))).andExpect(jsonPath("$.fotos", hasSize(0)));
    }

    @Test
    @DisplayName("una oferta sin fotos, o con la foto de otro prestador, no se publica: 400 DATO_INVALIDO (DC-47)")
    void ofertaSinFotosPropias() throws Exception {
        String deOtro = subirFoto("10000000-0000-0000-0000-000000000001", "AGENCIA_OPERADORA");

        http.perform(comoGuia(post("/prestador/ofertas")).contentType(MediaType.APPLICATION_JSON)
                        .content(NUEVA_ACTIVIDAD.replace("\"fotos\": [\"%s\"],", "")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("fotos: debe tener al menos una foto"));
        http.perform(comoGuia(post("/prestador/ofertas")).contentType(MediaType.APPLICATION_JSON)
                        .content(NUEVA_ACTIVIDAD.formatted(deOtro)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("DATO_INVALIDO"));
    }

    /** Sube una foto para las ofertas del prestador y devuelve su identificador. */
    @Test
    @DisplayName("[Demostración] el administrador registra una zona, el guía publica en ella, la zona se marca como "
            + "protegida y se inactiva: la oferta sigue visible, pero ya no se publica en esa zona (DC-49)")
    void zonasDelAdministrador() throws Exception {
        String zona = """
                {"nombre": "Isla de Salamanca", "descripcion": "Vía Parque entre Barranquilla y Ciénaga",
                 "ubicacion": {"latitud": 10.98, "longitud": -74.47}%s}""";
        String creada = http.perform(comoAdministrador(post("/admin/zonas")).contentType(MediaType.APPLICATION_JSON)
                        .content(zona.formatted("")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.activa").value(true))
                .andReturn().getResponse().getContentAsString();
        String zonaId = JsonPath.read(creada, "$.id");
        http.perform(comoAdministrador(post("/admin/zonas")).contentType(MediaType.APPLICATION_JSON)
                        .content(zona.formatted("").replace("Isla de Salamanca", "ISLA DE SALAMANCÁ")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("ZONA_DUPLICADA"));
        http.perform(get("/zonas")).andExpect(jsonPath("$[*].id", hasItem(zonaId)));

        String actividad = NUEVA_ACTIVIDAD.formatted(subirFoto(GUIA, "GUIA_INDEPENDIENTE"))
                .replace("a00000000000000000000007", zonaId);
        String oferta = http.perform(comoGuia(post("/prestador/ofertas")).contentType(MediaType.APPLICATION_JSON)
                        .content(actividad))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String ofertaId = JsonPath.read(oferta, "$.id");
        http.perform(post("/internal/avisos").contentType(MediaType.APPLICATION_JSON).content("""
                        {"idAviso": "demo-zona-1", "tipo": "MODERACION", "ofertaId": "%s", "numeroVersion": 1,
                         "decision": "APROBAR"}""".formatted(ofertaId)))
                .andExpect(jsonPath("$.resultado").value("APLICADO"));

        String protegida = ", \"areaProtegida\": true, \"buenasPracticas\": [\"No alimentar a los animales\"]";
        http.perform(comoAdministrador(put("/admin/zonas/" + zonaId)).contentType(MediaType.APPLICATION_JSON)
                        .content(zona.formatted(protegida + ", \"activa\": true")))
                .andExpect(status().isOk());
        http.perform(get("/ofertas/" + ofertaId))
                .andExpect(jsonPath("$.zona.areaProtegida").value(true))
                .andExpect(jsonPath("$.zona.buenasPracticas[0]").value("No alimentar a los animales"));

        http.perform(comoAdministrador(put("/admin/zonas/" + zonaId)).contentType(MediaType.APPLICATION_JSON)
                        .content(zona.formatted(protegida + ", \"activa\": false")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activa").value(false));
        http.perform(get("/zonas")).andExpect(jsonPath("$[*].id", not(hasItem(zonaId))));
        http.perform(get("/zonas/" + zonaId)).andExpect(status().isOk())
                .andExpect(jsonPath("$.activa").value(false));
        http.perform(get("/ofertas").param("zonaId", zonaId))
                .andExpect(jsonPath("$.contenido[*].id", hasItem(ofertaId)));
        http.perform(comoGuia(post("/prestador/ofertas")).contentType(MediaType.APPLICATION_JSON).content(actividad))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("ZONA_NO_DISPONIBLE"));
    }

    private static MockHttpServletRequestBuilder comoAdministrador(MockHttpServletRequestBuilder peticion) {
        return peticion.header("X-Usuario-Id", "60000000-0000-0000-0000-000000000006").header("X-Rol", "ADMINISTRADOR");
    }

    private String subirFoto(String prestadorId, String tipoPrestador) throws Exception {
        String foto = http.perform(multipart("/prestador/fotos")
                        .file(new MockMultipartFile("foto", "foto.jpg", "image/jpeg", ImagenesDePrueba.jpg(800, 600)))
                        .header("X-Usuario-Id", prestadorId).header("X-Rol", "PRESTADOR")
                        .header("X-Tipo-Prestador", tipoPrestador))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(foto, "$.id");
    }

    private static MockHttpServletRequestBuilder comoGuia(MockHttpServletRequestBuilder peticion) {
        return peticion.header("X-Usuario-Id", GUIA).header("X-Rol", "PRESTADOR")
                .header("X-Tipo-Prestador", "GUIA_INDEPENDIENTE");
    }

    @Test
    @DisplayName("las zonas de la precarga se listan ordenadas por nombre")
    void zonas() throws Exception {
        http.perform(get("/zonas"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(7)))
                .andExpect(jsonPath("$[0].nombre").value("Centro Histórico"));
    }
}
