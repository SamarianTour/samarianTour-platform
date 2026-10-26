package co.edu.unimagdalena.samariantour.nucleo.solicitudes.infraestructura.salida.catalogo;

import co.edu.unimagdalena.samariantour.nucleo.CatalogoSimulado;
import co.edu.unimagdalena.samariantour.nucleo.compartido.config.ClienteCatalogoConfig;
import co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.excepcion.CatalogoNoDisponibleException;
import co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.modelo.OfertaSolicitable;
import co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.modelo.TipoOferta;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.time.LocalTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/** La consulta de la oferta al catálogo (RF-30, ADR-04): nunca llama al catálogo real. */
class CatalogoConsultaRestAdapterTest {

    private static final String BASE = "http://catalogo/api/catalogo";
    private static final String CABO_SAN_JUAN = "b00000000000000000000001";
    private static final String OFERTA = """
            {"id": "b00000000000000000000001", "tipo": "ACTIVIDAD", "nombre": "Caminata a Cabo San Juan del Guía",
             "prestadorId": "10000000-0000-0000-0000-000000000001", "zonaId": "a00000000000000000000005",
             "horariosSalida": ["07:00", "08:00"], "capacidadMaxima": 12}""";

    private MockRestServiceServer catalogo;
    private CircuitBreaker circuito;
    private CatalogoConsultaRestAdapter adaptador;

    @BeforeEach
    void preparar() {
        RestClient.Builder clientes = RestClient.builder().baseUrl(BASE);
        catalogo = MockRestServiceServer.bindTo(clientes).build();
        circuito = CircuitBreaker.ofDefaults("catalogo");
        adaptador = new CatalogoConsultaRestAdapter(clientes.build(), circuito);
    }

    @Test
    @DisplayName("200: la oferta con lo que la solicitud copia y valida")
    void oferta() {
        catalogo.expect(requestTo(BASE + "/internal/ofertas/" + CABO_SAN_JUAN))
                .andRespond(withSuccess(OFERTA, MediaType.APPLICATION_JSON));

        OfertaSolicitable oferta = adaptador.oferta(CABO_SAN_JUAN).orElseThrow();

        assertThat(oferta.id()).isEqualTo(CABO_SAN_JUAN);
        assertThat(oferta.datos().tipo()).isEqualTo(TipoOferta.ACTIVIDAD);
        assertThat(oferta.datos().nombre()).isEqualTo("Caminata a Cabo San Juan del Guía");
        assertThat(oferta.datos().prestadorId()).isEqualTo(UUID.fromString("10000000-0000-0000-0000-000000000001"));
        assertThat(oferta.horariosSalida()).containsExactly(LocalTime.of(7, 0), LocalTime.of(8, 0));
        assertThat(oferta.capacidadMaxima()).isEqualTo(12);
    }

    @Test
    @DisplayName("404: no está visible; no cuenta como falla del circuito")
    void noVisible() {
        catalogo.expect(requestTo(BASE + "/internal/ofertas/x")).andRespond(withStatus(HttpStatus.NOT_FOUND));

        assertThat(adaptador.oferta("x")).isEmpty();
        assertThat(circuito.getMetrics().getNumberOfFailedCalls()).isZero();
    }

    @Test
    @DisplayName("5xx o una respuesta que no es del contrato: catálogo no disponible, y cuenta como falla")
    void error() {
        catalogo.expect(requestTo(BASE + "/internal/ofertas/" + CABO_SAN_JUAN)).andRespond(withServerError());
        catalogo.expect(requestTo(BASE + "/internal/ofertas/" + CABO_SAN_JUAN))
                .andRespond(withSuccess("{\"id\": \"b1\", \"tipo\": \"CRUCERO\", \"capacidadMaxima\": 2}",
                        MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> adaptador.oferta(CABO_SAN_JUAN)).isInstanceOf(CatalogoNoDisponibleException.class)
                .extracting(e -> ((CatalogoNoDisponibleException) e).codigo()).isEqualTo("CATALOGO_NO_DISPONIBLE");
        assertThatThrownBy(() -> adaptador.oferta(CABO_SAN_JUAN)).isInstanceOf(CatalogoNoDisponibleException.class)
                .hasMessageContaining("formato");
        assertThat(circuito.getMetrics().getNumberOfFailedCalls()).isEqualTo(2);
    }

    @Test
    @DisplayName("con el circuito abierto responde no disponible sin llamar al catálogo")
    void circuitoAbierto() {
        circuito.transitionToOpenState();

        assertThatThrownBy(() -> adaptador.oferta(CABO_SAN_JUAN)).isInstanceOf(CatalogoNoDisponibleException.class)
                .hasMessageContaining("circuito");
        catalogo.verify();
    }

    @Test
    @DisplayName("un catálogo que no responde en 3 s cuenta como no disponible (RNF-05), con el cliente compartido")
    void tiempoAgotado() {
        try (CatalogoSimulado lento = new CatalogoSimulado()) {
            lento.demorar(Duration.ofSeconds(5));
            CatalogoConsultaRestAdapter conEspera = new CatalogoConsultaRestAdapter(
                    ClienteCatalogoConfig.crear(lento.url()), CircuitBreaker.ofDefaults("catalogo"));
            long inicio = System.nanoTime();

            assertThatThrownBy(() -> conEspera.oferta(CABO_SAN_JUAN)).isInstanceOf(CatalogoNoDisponibleException.class)
                    .hasMessageContaining("a tiempo");
            assertThat(Duration.ofNanos(System.nanoTime() - inicio)).isLessThan(Duration.ofSeconds(4));
        }
    }
}
