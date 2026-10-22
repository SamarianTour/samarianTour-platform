package co.edu.unimagdalena.samariantour.catalogo;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * El servicio completo con los perfiles de la demostración: controladores, casos de uso y adaptadores en memoria
 * con los datos de Santa Marta. No necesita MongoDB ni Docker.
 */
@SpringBootTest(properties = "spring.autoconfigure.exclude="
        + "org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration,"
        + "org.springframework.boot.autoconfigure.data.mongo.MongoDataAutoConfiguration")
@AutoConfigureMockMvc
@ActiveProfiles({"memoria", "demo"})
class DemostracionApiTest {

    private static final String GUIA = "20000000-0000-0000-0000-000000000002";

    private static final String NUEVA_ACTIVIDAD = """
            {
              "tipo": "ACTIVIDAD",
              "nombre": "Caminata nocturna en Minca",
              "descripcion": "Recorrido para observar ranas e insectos al caer la noche",
              "precioReferencia": 75000,
              "zonaId": "a00000000000000000000007",
              "capacidadMaxima": 6,
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
                .andExpect(jsonPath("$.perfilPrestador.descripcion").exists());

        String creada = http.perform(post("/prestador/ofertas").contentType(MediaType.APPLICATION_JSON)
                        .header("X-Usuario-Id", GUIA).header("X-Rol", "PRESTADOR")
                        .header("X-Tipo-Prestador", "GUIA_INDEPENDIENTE")
                        .content(NUEVA_ACTIVIDAD))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.pendiente.numero").value(1))
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
                .andExpect(jsonPath("$.contenido[*].id", hasItem(id)));
        http.perform(get("/ofertas/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.precioReferencia").value(75000))
                .andExpect(jsonPath("$.actividad.horariosSalida[0]").value("18:30"));

        http.perform(put("/prestador/ofertas/" + id).contentType(MediaType.APPLICATION_JSON)
                        .header("X-Usuario-Id", GUIA).header("X-Rol", "PRESTADOR")
                        .header("X-Tipo-Prestador", "GUIA_INDEPENDIENTE")
                        .content(NUEVA_ACTIVIDAD.replace("75000", "82000")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.publicada.numero").value(1))
                .andExpect(jsonPath("$.pendiente.numero").value(2));
        http.perform(get("/ofertas/" + id)).andExpect(jsonPath("$.precioReferencia").value(75000));
    }

    @Test
    @DisplayName("[Demostración] retirar una oferta aprobada: sale de la búsqueda, sigue en la lista del prestador "
            + "como retirada y ya no se puede editar")
    void retiro() throws Exception {
        String creada = http.perform(post("/prestador/ofertas").contentType(MediaType.APPLICATION_JSON)
                        .header("X-Usuario-Id", GUIA).header("X-Rol", "PRESTADOR")
                        .header("X-Tipo-Prestador", "GUIA_INDEPENDIENTE")
                        .content(NUEVA_ACTIVIDAD))
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
                        .content(NUEVA_ACTIVIDAD))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("OFERTA_NO_EDITABLE"));
    }

    @Test
    @DisplayName("un prestador con el RNT inactivo no puede publicar: 403 RNT_INACTIVO")
    void prestadorConRntInactivo() throws Exception {
        http.perform(post("/prestador/ofertas").contentType(MediaType.APPLICATION_JSON)
                        .header("X-Usuario-Id", "50000000-0000-0000-0000-000000000005").header("X-Rol", "PRESTADOR")
                        .header("X-Tipo-Prestador", "AGENCIA_OPERADORA")
                        .content(NUEVA_ACTIVIDAD))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.codigo").value("RNT_INACTIVO"));
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
