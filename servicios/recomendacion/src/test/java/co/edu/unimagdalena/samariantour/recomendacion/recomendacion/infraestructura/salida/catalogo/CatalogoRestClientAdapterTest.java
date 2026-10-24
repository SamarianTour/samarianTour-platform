package co.edu.unimagdalena.samariantour.recomendacion.recomendacion.infraestructura.salida.catalogo;

import co.edu.unimagdalena.samariantour.recomendacion.recomendacion.dominio.excepcion.RecomendacionNoDisponibleException;
import co.edu.unimagdalena.samariantour.recomendacion.recomendacion.dominio.modelo.NivelEsfuerzo;
import co.edu.unimagdalena.samariantour.recomendacion.recomendacion.dominio.modelo.OfertaCandidata;
import co.edu.unimagdalena.samariantour.recomendacion.recomendacion.dominio.modelo.TipoActividad;
import co.edu.unimagdalena.samariantour.recomendacion.recomendacion.dominio.modelo.TipoOferta;
import com.github.benmanes.caffeine.cache.Ticker;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class CatalogoRestClientAdapterTest {

    private static final String BASE = "http://catalogo/api/catalogo";
    private static final String ZONAS = """
            [{"id": "a1", "nombre": "Minca", "areaProtegida": false, "altaAfluencia": false, "activa": true,
              "buenasPracticas": []}]""";

    private final AtomicLong nanos = new AtomicLong();
    private final Ticker reloj = nanos::get;
    private MockRestServiceServer servidor;
    private CatalogoRestClientAdapter adaptador;

    @BeforeEach
    void preparar() {
        RestClient.Builder clientes = RestClient.builder().baseUrl(BASE);
        servidor = MockRestServiceServer.bindTo(clientes).build();
        // Ejecutor directo: la lectura en segundo plano termina antes de seguir con la prueba.
        adaptador = new CatalogoRestClientAdapter(clientes.build(), Duration.ofSeconds(60), reloj, Runnable::run);
    }

    @Test
    @DisplayName("lee todas las páginas de a 100, con el nombre de la zona y sin datos que no son criterios")
    void leeTodasLasPaginas() {
        esperarZonas();
        esperarPagina(0, 2, oferta("b1", "ACTIVIDAD", "a1", "[\"SENDERISMO\"]", "\"MODERADO\""));
        esperarPagina(1, 2, oferta("b2", "RESTAURANTE", "a1", "[]", "null"));

        List<OfertaCandidata> ofertas = adaptador.ofertasVisibles();

        servidor.verify();
        assertThat(ofertas).extracting(OfertaCandidata::id).containsExactly("b1", "b2");
        OfertaCandidata caminata = ofertas.getFirst();
        assertThat(caminata.tipo()).isEqualTo(TipoOferta.ACTIVIDAD);
        assertThat(caminata.zonaNombre()).isEqualTo("Minca");
        assertThat(caminata.tiposActividad()).containsExactly(TipoActividad.SENDERISMO);
        assertThat(caminata.nivelEsfuerzo()).isEqualTo(NivelEsfuerzo.MODERADO);
        assertThat(caminata.precioReferencia()).isEqualByComparingTo("80000");
        assertThat(caminata.prestadorId()).isEqualTo(UUID.fromString("20000000-0000-0000-0000-000000000002"));
        assertThat(ofertas.get(1).nivelEsfuerzo()).isNull();
    }

    @Test
    @DisplayName("el nombre de una zona inactiva se pide a /zonas/{id} una sola vez; si no existe, queda sin nombre")
    void zonaInactiva() {
        esperarZonas();
        esperarPagina(0, 1, oferta("b1", "ACTIVIDAD", "a9", "[]", "null") + ","
                + oferta("b2", "ACTIVIDAD", "a9", "[]", "null") + "," + oferta("b3", "ACTIVIDAD", "a8", "[]", "null"));
        servidor.expect(requestTo(BASE + "/zonas/a9"))
                .andRespond(withSuccess("{\"id\": \"a9\", \"nombre\": \"Bahía Concha\", \"activa\": false}",
                        MediaType.APPLICATION_JSON));
        servidor.expect(requestTo(BASE + "/zonas/a8")).andRespond(withStatus(HttpStatus.NOT_FOUND));

        List<OfertaCandidata> ofertas = adaptador.ofertasVisibles();

        servidor.verify();
        assertThat(ofertas).extracting(OfertaCandidata::zonaNombre).containsExactly("Bahía Concha", "Bahía Concha", null);
    }

    @Test
    @DisplayName("un tipo de actividad nuevo se ignora; un tipo de oferta o un nivel desconocido descartan la oferta")
    void valoresDesconocidos() {
        esperarZonas();
        esperarPagina(0, 1, oferta("b1", "ACTIVIDAD", "a1", "[\"SENDERISMO\", \"PARAPENTE\"]", "\"BAJO\"") + ","
                + oferta("b2", "CRUCERO", "a1", "[]", "null") + ","
                + oferta("b3", "ACTIVIDAD", "a1", "[]", "\"EXTREMO\""));

        List<OfertaCandidata> ofertas = adaptador.ofertasVisibles();

        assertThat(ofertas).extracting(OfertaCandidata::id).containsExactly("b1");
        assertThat(ofertas.getFirst().tiposActividad()).containsExactly(TipoActividad.SENDERISMO);
    }

    @Test
    @DisplayName("dentro de los 60 s responde la caché, sin volver al catálogo")
    void cache() {
        esperarZonas();
        esperarPagina(0, 1, oferta("b1", "ACTIVIDAD", "a1", "[]", "null"));

        adaptador.ofertasVisibles();
        nanos.addAndGet(Duration.ofSeconds(59).toNanos());
        assertThat(adaptador.ofertasVisibles()).hasSize(1);

        servidor.verify();
    }

    @Test
    @DisplayName("vencida la caché se vuelve a leer el catálogo (aquí la lectura en segundo plano corre en el mismo hilo)")
    void refresco() {
        esperarZonas();
        esperarPagina(0, 1, oferta("b1", "ACTIVIDAD", "a1", "[]", "null"));
        esperarZonas();
        esperarPagina(0, 1, oferta("b2", "ACTIVIDAD", "a1", "[]", "null"));

        assertThat(adaptador.ofertasVisibles()).extracting(OfertaCandidata::id).containsExactly("b1");
        nanos.addAndGet(Duration.ofSeconds(61).toNanos());
        assertThat(adaptador.ofertasVisibles()).extracting(OfertaCandidata::id).containsExactly("b2");
        assertThat(adaptador.ofertasVisibles()).extracting(OfertaCandidata::id).containsExactly("b2");

        servidor.verify();
    }

    @Test
    @DisplayName("si el catálogo falla y hay copia, se usa aunque esté vencida")
    void copiaVencida() {
        esperarZonas();
        esperarPagina(0, 1, oferta("b1", "ACTIVIDAD", "a1", "[]", "null"));
        servidor.expect(requestTo(BASE + "/zonas")).andRespond(withServerError());

        adaptador.ofertasVisibles();
        nanos.addAndGet(Duration.ofSeconds(61).toNanos());

        assertThat(adaptador.ofertasVisibles()).extracting(OfertaCandidata::id).containsExactly("b1");
        assertThat(adaptador.ofertasVisibles()).extracting(OfertaCandidata::id).containsExactly("b1");
        servidor.verify();
    }

    @Test
    @DisplayName("si el catálogo falla y no hay copia: RECOMENDACION_NO_DISPONIBLE")
    void sinCopia() {
        servidor.expect(requestTo(BASE + "/zonas")).andRespond(withServerError());

        assertThatThrownBy(adaptador::ofertasVisibles)
                .isInstanceOf(RecomendacionNoDisponibleException.class)
                .extracting(e -> ((RecomendacionNoDisponibleException) e).codigo())
                .isEqualTo("RECOMENDACION_NO_DISPONIBLE");
    }

    private void esperarZonas() {
        servidor.expect(requestTo(BASE + "/zonas")).andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(ZONAS, MediaType.APPLICATION_JSON));
    }

    private void esperarPagina(int pagina, int totalPaginas, String ofertas) {
        servidor.expect(requestTo(BASE + "/internal/ofertas?page=" + pagina + "&size=100"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("""
                        {"contenido": [%s], "pagina": %d, "tamano": 100, "totalElementos": 2, "totalPaginas": %d}"""
                        .formatted(ofertas, pagina, totalPaginas), MediaType.APPLICATION_JSON));
    }

    private static String oferta(String id, String tipo, String zonaId, String tiposActividad, String nivel) {
        return """
                {"id": "%s", "tipo": "%s", "nombre": "Oferta %s", "descripcion": "Descripción",
                 "prestadorId": "20000000-0000-0000-0000-000000000002", "zonaId": "%s", "zonaAltaAfluencia": false,
                 "precioReferencia": 80000, "unidadPrecio": "por persona", "tiposActividad": %s,
                 "nivelEsfuerzo": %s, "accesibleSillaRuedas": true, "calificacion": 5}"""
                .formatted(id, tipo, id, zonaId, tiposActividad, nivel);
    }
}
