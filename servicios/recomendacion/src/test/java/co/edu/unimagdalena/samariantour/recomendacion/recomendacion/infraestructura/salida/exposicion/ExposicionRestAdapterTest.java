package co.edu.unimagdalena.samariantour.recomendacion.recomendacion.infraestructura.salida.exposicion;

import co.edu.unimagdalena.samariantour.recomendacion.recomendacion.dominio.modelo.CriterioAfinidad;
import co.edu.unimagdalena.samariantour.recomendacion.recomendacion.dominio.modelo.Explicacion;
import co.edu.unimagdalena.samariantour.recomendacion.recomendacion.dominio.modelo.MotorRecomendacion;
import co.edu.unimagdalena.samariantour.recomendacion.recomendacion.dominio.modelo.Recomendacion;
import co.edu.unimagdalena.samariantour.recomendacion.recomendacion.dominio.modelo.RegistroExposicion;
import co.edu.unimagdalena.samariantour.recomendacion.recomendacion.dominio.modelo.ResultadoRecomendacion;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static co.edu.unimagdalena.samariantour.recomendacion.recomendacion.OfertasDePrueba.oferta;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;

/** El registro de exposición al núcleo, con el log de respaldo (RET-02, DR-08, DR-24). */
class ExposicionRestAdapterTest {

    private static final String BASE = "http://nucleo/api/nucleo";
    private static final UUID GUIA = UUID.fromString("20000000-0000-0000-0000-000000000002");

    private final List<RegistroExposicion> respaldados = new ArrayList<>();
    private MockRestServiceServer servidor;
    private ExposicionRestAdapter adaptador;

    @BeforeEach
    void preparar() {
        RestClient.Builder clientes = RestClient.builder().baseUrl(BASE);
        servidor = MockRestServiceServer.bindTo(clientes).build();
        adaptador = new ExposicionRestAdapter(clientes.build(), respaldados::add);
    }

    @Test
    @DisplayName("envía la consulta al núcleo: fecha, origen, motor y, por oferta, su id, prestador y posición")
    void enviar() {
        servidor.expect(requestTo(BASE + "/internal/exposiciones")).andExpect(method(HttpMethod.POST))
                .andExpect(content().json("""
                        {"fecha": "2026-11-02T15:00:00Z", "origen": "RECOMENDACION", "motor": "IA",
                         "ofertas": [{"ofertaId": "b1", "prestadorId": "%s", "posicion": 1}]}""".formatted(GUIA), true))
                .andRespond(withStatus(HttpStatus.ACCEPTED));

        adaptador.registrar(registro());

        servidor.verify();
        assertThat(respaldados).isEmpty();
    }

    @Test
    @DisplayName("si el núcleo responde un error, el registro queda en el log y no lanza")
    void respaldo() {
        servidor.expect(requestTo(BASE + "/internal/exposiciones")).andRespond(withServerError()
                .contentType(MediaType.APPLICATION_JSON));
        RegistroExposicion registro = registro();

        adaptador.registrar(registro);

        assertThat(respaldados).containsExactly(registro);
    }

    private static RegistroExposicion registro() {
        return RegistroExposicion.de(Instant.parse("2026-11-02T15:00:00Z"), new ResultadoRecomendacion(
                MotorRecomendacion.IA, null, List.of(new Recomendacion(1, oferta("b1").prestador(GUIA).crear(),
                        new Explicacion(List.of(CriterioAfinidad.ZONA), "Queda en Minca, la zona que buscas.")))));
    }
}
