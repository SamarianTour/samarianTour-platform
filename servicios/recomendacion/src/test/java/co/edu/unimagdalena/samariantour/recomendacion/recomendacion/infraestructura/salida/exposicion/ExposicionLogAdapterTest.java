package co.edu.unimagdalena.samariantour.recomendacion.recomendacion.infraestructura.salida.exposicion;

import co.edu.unimagdalena.samariantour.recomendacion.recomendacion.dominio.modelo.CriterioAfinidad;
import co.edu.unimagdalena.samariantour.recomendacion.recomendacion.dominio.modelo.Explicacion;
import co.edu.unimagdalena.samariantour.recomendacion.recomendacion.dominio.modelo.MotorRecomendacion;
import co.edu.unimagdalena.samariantour.recomendacion.recomendacion.dominio.modelo.OrigenExposicion;
import co.edu.unimagdalena.samariantour.recomendacion.recomendacion.dominio.modelo.Recomendacion;
import co.edu.unimagdalena.samariantour.recomendacion.recomendacion.dominio.modelo.RegistroExposicion;
import co.edu.unimagdalena.samariantour.recomendacion.recomendacion.dominio.modelo.ResultadoAlternativas;
import co.edu.unimagdalena.samariantour.recomendacion.recomendacion.dominio.modelo.ResultadoRecomendacion;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static co.edu.unimagdalena.samariantour.recomendacion.recomendacion.OfertasDePrueba.oferta;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

class ExposicionLogAdapterTest {

    private static final UUID GUIA = UUID.fromString("20000000-0000-0000-0000-000000000002");
    private static final UUID AGENCIA = UUID.fromString("10000000-0000-0000-0000-000000000001");

    private final ObjectMapper json = new ObjectMapper();
    private final List<String> lineas = new ArrayList<>();

    @Test
    @DisplayName("una línea JSON por consulta: fecha, origen, motor y, por oferta, su id, prestador, posición y criterios")
    void lineaJson() throws Exception {
        ResultadoRecomendacion resultado = new ResultadoRecomendacion(MotorRecomendacion.IA, null, List.of(
                new Recomendacion(1, oferta("b1").prestador(GUIA).descripcion("Caminata con Ana").crear(),
                        new Explicacion(List.of(CriterioAfinidad.ZONA), "Queda en Minca, la zona que buscas.")),
                new Recomendacion(2, oferta("b2").prestador(AGENCIA).crear(),
                        new Explicacion(List.of(), "Una de las ofertas visibles."))));

        new ExposicionLogAdapter(json, lineas::add)
                .registrar(RegistroExposicion.de(Instant.parse("2026-10-24T15:00:00Z"), resultado));

        assertThat(lineas).hasSize(1);
        JsonNode linea = json.readTree(lineas.getFirst());
        assertThat(linea.get("fecha").asText()).isEqualTo("2026-10-24T15:00:00Z");
        assertThat(linea.get("origen").asText()).isEqualTo("RECOMENDACION");
        assertThat(linea.get("motor").asText()).isEqualTo("IA");
        JsonNode primera = linea.get("recomendaciones").get(0);
        assertThat(primera.get("ofertaId").asText()).isEqualTo("b1");
        assertThat(primera.get("prestadorId").asText()).isEqualTo(GUIA.toString());
        assertThat(primera.get("posicion").asInt()).isEqualTo(1);
        assertThat(primera.get("criterios").get(0).asText()).isEqualTo("ZONA");
        assertThat(linea.get("recomendaciones").get(1).get("prestadorId").asText()).isEqualTo(AGENCIA.toString());
    }

    @Test
    @DisplayName("no lleva textos: ni las explicaciones ni las descripciones (ni, por construcción, nada del turista)")
    void sinTextos() {
        ResultadoRecomendacion resultado = new ResultadoRecomendacion(MotorRecomendacion.REGLAS, null, List.of(
                new Recomendacion(1, oferta("b1").descripcion("Caminata con Ana").crear(),
                        new Explicacion(List.of(), "Coincide con lo que buscas, Ana."))));

        new ExposicionLogAdapter(json, lineas::add).registrar(RegistroExposicion.de(Instant.now(), resultado));

        assertThat(lineas.getFirst()).doesNotContain("Ana", "explicacion", "texto", "descripcion");
    }

    @Test
    @DisplayName("las alternativas se registran con el origen ALTERNATIVAS y el motor por reglas (DR-20)")
    void origenAlternativas() throws Exception {
        ResultadoAlternativas resultado = new ResultadoAlternativas(true, null,
                new ResultadoAlternativas.ZonaOrigen("a5", "Parque Tayrona"), List.of(
                new Recomendacion(1, oferta("b7").prestador(GUIA).crear(),
                        new Explicacion(List.of(CriterioAfinidad.TIPO_OFERTA), "Mismo tipo de oferta."))));

        new ExposicionLogAdapter(json, lineas::add)
                .registrar(RegistroExposicion.de(Instant.parse("2026-10-24T15:00:00Z"), resultado));

        JsonNode linea = json.readTree(lineas.getFirst());
        assertThat(linea.get("origen").asText()).isEqualTo("ALTERNATIVAS");
        assertThat(linea.get("motor").asText()).isEqualTo("REGLAS");
        assertThat(linea.get("recomendaciones").get(0).get("ofertaId").asText()).isEqualTo("b7");
        assertThat(linea.get("recomendaciones").get(0).get("criterios").get(0).asText()).isEqualTo("TIPO_OFERTA");
    }

    @Test
    @DisplayName("si el destino falla, no lanza: la recomendación igual llega al turista")
    void nuncaLanza() {
        ExposicionLogAdapter adaptador = new ExposicionLogAdapter(json, linea -> {
            throw new IllegalStateException("disco lleno");
        });
        RegistroExposicion registro = new RegistroExposicion(Instant.now(), OrigenExposicion.RECOMENDACION,
                MotorRecomendacion.REGLAS, List.of());

        assertThatCode(() -> adaptador.registrar(registro)).doesNotThrowAnyException();
    }
}
