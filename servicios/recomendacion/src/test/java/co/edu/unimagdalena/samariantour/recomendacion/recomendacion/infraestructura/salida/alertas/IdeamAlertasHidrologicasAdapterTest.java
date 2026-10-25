package co.edu.unimagdalena.samariantour.recomendacion.recomendacion.infraestructura.salida.alertas;

import co.edu.unimagdalena.samariantour.recomendacion.compartido.http.ClientesHttp;
import co.edu.unimagdalena.samariantour.recomendacion.recomendacion.dominio.excepcion.FuenteAlertasNoDisponibleException;
import co.edu.unimagdalena.samariantour.recomendacion.recomendacion.dominio.modelo.AlertaClimatica;
import co.edu.unimagdalena.samariantour.recomendacion.recomendacion.dominio.modelo.NivelAlerta;
import co.edu.unimagdalena.samariantour.recomendacion.recomendacion.dominio.modelo.ResultadoFuenteAlertas;
import co.edu.unimagdalena.samariantour.recomendacion.recomendacion.dominio.modelo.TipoAlerta;
import co.edu.unimagdalena.samariantour.recomendacion.recomendacion.dominio.modelo.ZonaCatalogo;
import com.sun.net.httpserver.HttpServer;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.client.ResponseActions;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.queryParam;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/**
 * Nunca llama al IDEAM: las respuestas están grabadas en {@code src/test/resources/ideam}. Las de Centro Histórico,
 * Troncal del Caribe, sin features y el error son reales (24-10-2026); las demás tienen el mismo formato.
 */
class IdeamAlertasHidrologicasAdapterTest {

    private static final String CAPA = "https://ideam.prueba/arcgis/MapServer/2";
    private static final ZonaCatalogo CENTRO = new ZonaCatalogo("a00000000000000000000001", "Centro Histórico",
            11.2447, -74.211);
    private static final ZonaCatalogo TRONCAL = new ZonaCatalogo("a00000000000000000000006", "Troncal del Caribe",
            11.257, -73.77);

    private MockRestServiceServer servidor;
    private IdeamAlertasHidrologicasAdapter adaptador;

    @BeforeEach
    void preparar() {
        RestClient.Builder clientes = RestClient.builder().baseUrl(CAPA);
        servidor = MockRestServiceServer.bindTo(clientes).build();
        adaptador = new IdeamAlertasHidrologicasAdapter(clientes.build(), CircuitBreaker.ofDefaults("alertas"));
    }

    @Test
    @DisplayName("alerta amarilla real: una alerta HIDROLOGICA con la subzona, la fuente del IDEAM y sin fechas")
    void alertaPorNivel() {
        responder(CENTRO, "centro-historico.json");

        ResultadoFuenteAlertas resultado = adaptador.consultar(List.of(CENTRO));

        servidor.verify();
        assertThat(resultado.zonasFallidas()).isEmpty();
        AlertaClimatica alerta = resultado.alertas().getFirst();
        assertThat(alerta.zonaId()).isEqualTo(CENTRO.id());
        assertThat(alerta.tipo()).isEqualTo(TipoAlerta.HIDROLOGICA);
        assertThat(alerta.nivel()).isEqualTo(NivelAlerta.AMARILLA);
        assertThat(alerta.titulo()).isEqualTo("Alerta hidrológica en la subzona Río Piedras - Río Manzanares");
        assertThat(alerta.fuente()).isEqualTo("IDEAM, alertas hidrológicas vigentes");
        assertThat(alerta.emitidaEn()).as("la capa no tiene fechas: no se inventan").isNull();
        assertThat(alerta.vigenteHasta()).isNull();
    }

    @Test
    @DisplayName("la URL lleva el punto como longitud,latitud, sin geometría de vuelta y solo con NOMSZH y NIVEL_A")
    void ordenDeLasCoordenadas() {
        servidor.expect(requestTo(org.hamcrest.Matchers.startsWith(CAPA + "/query?")))
                .andExpect(queryParam("geometry", "-74.211,11.2447"))
                .andExpect(queryParam("geometryType", "esriGeometryPoint"))
                .andExpect(queryParam("inSR", "4326"))
                .andExpect(queryParam("spatialRel", "esriSpatialRelIntersects"))
                .andExpect(queryParam("outFields", "NOMSZH,NIVEL_A"))
                .andExpect(queryParam("returnGeometry", "false"))
                .andExpect(queryParam("f", "json"))
                .andRespond(withSuccess(grabada("sin-features.json"), MediaType.APPLICATION_JSON));

        adaptador.consultar(List.of(CENTRO));

        servidor.verify();
    }

    @Test
    @DisplayName("sin features, condiciones normales o un nivel desconocido: la zona queda sin alerta, no fallida")
    void sinAlerta() {
        for (String grabada : List.of("sin-features.json", "condiciones-normales.json", "nivel-desconocido.json")) {
            servidor.reset();
            responder(CENTRO, grabada);

            ResultadoFuenteAlertas resultado = adaptador.consultar(List.of(CENTRO));

            assertThat(resultado.alertas()).as(grabada).isEmpty();
            assertThat(resultado.zonasFallidas()).as(grabada).isEmpty();
        }
    }

    @Test
    @DisplayName("si caen varias features en el punto, queda la de mayor nivel con su subzona")
    void variasFeatures() {
        responder(CENTRO, "varias-features.json");

        AlertaClimatica alerta = adaptador.consultar(List.of(CENTRO)).alertas().getFirst();

        assertThat(alerta.nivel()).isEqualTo(NivelAlerta.ROJA);
        assertThat(alerta.titulo()).endsWith("Río Gaira");
    }

    @Test
    @DisplayName("si una zona falla (5xx), las demás se leen y esa queda como fallida para conservar lo anterior")
    void unaZonaFalla() {
        expectZona(CENTRO).andRespond(withServerError());
        responder(TRONCAL, "troncal-del-caribe.json");

        ResultadoFuenteAlertas resultado = adaptador.consultar(List.of(CENTRO, TRONCAL));

        assertThat(resultado.zonasFallidas()).containsExactly(CENTRO.id());
        assertThat(resultado.alertas()).singleElement().satisfies(alerta -> {
            assertThat(alerta.zonaId()).isEqualTo(TRONCAL.id());
            assertThat(alerta.titulo()).endsWith("Rio Guachaca - Mendiguaca y Buritaca");
        });
    }

    @Test
    @DisplayName("un error de ArcGIS llega con HTTP 200 y el objeto error: es una falla de la zona")
    void errorDeArcgis() {
        expectZona(CENTRO).andRespond(withSuccess(grabada("error-arcgis.json"), MediaType.APPLICATION_JSON));
        responder(TRONCAL, "troncal-del-caribe.json");

        assertThat(adaptador.consultar(List.of(CENTRO, TRONCAL)).zonasFallidas()).containsExactly(CENTRO.id());
    }

    @Test
    @DisplayName("si fallan todas las zonas, la fuente falla y la caché conserva la lectura anterior")
    void todasFallan() {
        expectZona(CENTRO).andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE));
        expectZona(TRONCAL).andRespond(withServerError());

        assertThatThrownBy(() -> adaptador.consultar(List.of(CENTRO, TRONCAL)))
                .isInstanceOf(FuenteAlertasNoDisponibleException.class)
                .extracting(e -> ((FuenteAlertasNoDisponibleException) e).causa()).isEqualTo("HTTP_500");
    }

    @Test
    @DisplayName("las zonas sin ubicación no se consultan; sin ninguna con ubicación, la fuente falla")
    void sinUbicacion() {
        ZonaCatalogo sinUbicacion = new ZonaCatalogo("a00000000000000000000099", "Sin ubicación", null, null);

        assertThatThrownBy(() -> adaptador.consultar(List.of(sinUbicacion)))
                .isInstanceOf(FuenteAlertasNoDisponibleException.class);
        responder(CENTRO, "centro-historico.json");
        assertThat(adaptador.consultar(List.of(sinUbicacion, CENTRO)).alertas()).hasSize(1);
        servidor.verify();
    }

    @Test
    @DisplayName("un IDEAM que no responde a tiempo es TIMEOUT, cortado al tiempo máximo")
    void tiempoAgotado() throws IOException {
        HttpServer lento = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        lento.createContext("/arcgis/MapServer/2/query", intercambio -> {
            try {
                Thread.sleep(3_000);
                intercambio.sendResponseHeaders(200, -1);
            } catch (InterruptedException | IOException e) {
                Thread.currentThread().interrupt();
            } finally {
                intercambio.close();
            }
        });
        lento.start();
        try {
            RestClient cliente = RestClient.builder()
                    .baseUrl("http://127.0.0.1:" + lento.getAddress().getPort() + "/arcgis/MapServer/2")
                    .requestFactory(ClientesHttp.conTiempoMaximo(Duration.ofMillis(300)))
                    .build();
            IdeamAlertasHidrologicasAdapter conTiempo =
                    new IdeamAlertasHidrologicasAdapter(cliente, CircuitBreaker.ofDefaults("alertas"));
            long inicio = System.nanoTime();

            assertThatThrownBy(() -> conTiempo.consultar(List.of(CENTRO)))
                    .isInstanceOf(FuenteAlertasNoDisponibleException.class)
                    .extracting(e -> ((FuenteAlertasNoDisponibleException) e).causa()).isEqualTo("TIMEOUT");
            assertThat(Duration.ofNanos(System.nanoTime() - inicio)).isLessThan(Duration.ofSeconds(2));
        } finally {
            lento.stop(0);
        }
    }

    private void responder(ZonaCatalogo zona, String grabada) {
        expectZona(zona).andRespond(withSuccess(grabada(grabada), MediaType.APPLICATION_JSON));
    }

    private ResponseActions expectZona(ZonaCatalogo zona) {
        return servidor.expect(requestTo(org.hamcrest.Matchers.startsWith(CAPA + "/query?")))
                .andExpect(queryParam("geometry", zona.longitud() + "," + zona.latitud()));
    }

    private static String grabada(String nombre) {
        try {
            return new ClassPathResource("ideam/" + nombre).getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }
}
