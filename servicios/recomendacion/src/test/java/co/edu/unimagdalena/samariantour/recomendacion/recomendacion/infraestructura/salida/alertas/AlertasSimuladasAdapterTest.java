package co.edu.unimagdalena.samariantour.recomendacion.recomendacion.infraestructura.salida.alertas;

import co.edu.unimagdalena.samariantour.recomendacion.recomendacion.dominio.excepcion.FuenteAlertasNoDisponibleException;
import co.edu.unimagdalena.samariantour.recomendacion.recomendacion.dominio.modelo.AlertaClimatica;
import co.edu.unimagdalena.samariantour.recomendacion.recomendacion.dominio.modelo.NivelAlerta;
import co.edu.unimagdalena.samariantour.recomendacion.recomendacion.dominio.modelo.TipoAlerta;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.ClassPathResource;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AlertasSimuladasAdapterTest {

    private static final Instant AHORA = Instant.parse("2026-10-24T15:00:00Z");
    private final ObjectMapper json = new ObjectMapper().registerModule(new JavaTimeModule());

    @Test
    @DisplayName("lee las alertas del archivo de la demo, con los ids de zona del catálogo y la fuente marcada como simulada")
    void archivoDeLaDemo() {
        List<AlertaClimatica> alertas = adaptador(new ClassPathResource("alertas/alertas-simuladas.json"), AHORA)
                .consultar(List.of()).alertas();

        assertThat(alertas).hasSize(4).allSatisfy(alerta -> assertThat(alerta.fuente()).endsWith("(simulado)"));
        AlertaClimatica tayrona = porId(alertas, "simulada-tayrona-cierre");
        assertThat(tayrona.zonaId()).isEqualTo("a00000000000000000000005");
        assertThat(tayrona.tipo()).isEqualTo(TipoAlerta.CIERRE);
        assertThat(tayrona.nivel()).isEqualTo(NivelAlerta.NARANJA);
        assertThat(tayrona.titulo()).isEqualTo("Cierre temporal del parque");
        assertThat(porId(alertas, "simulada-minca-deslizamiento").zonaId()).isEqualTo("a00000000000000000000007");
        assertThat(porId(alertas, "simulada-taganga-oleaje").zonaId()).isEqualTo("a00000000000000000000003");
        assertThat(alertas.stream().filter(a -> a.activa(AHORA))).hasSize(3);
        assertThat(porId(alertas, "simulada-centro-lluvias-vencida").activa(AHORA)).isFalse();
    }

    @Test
    @DisplayName("las fechas se calculan con el reloj en cada lectura: la demo nunca queda vencida")
    void fechasRelativas() {
        Instant dentroDeUnMes = AHORA.plus(Duration.ofDays(30));
        AlertaClimatica minca = porId(adaptador(new ClassPathResource("alertas/alertas-simuladas.json"), dentroDeUnMes)
                .consultar(List.of()).alertas(), "simulada-minca-deslizamiento");

        assertThat(minca.emitidaEn()).isEqualTo(dentroDeUnMes.minus(Duration.ofHours(5)));
        assertThat(minca.vigenteHasta()).isEqualTo(minca.emitidaEn().plus(Duration.ofDays(1)));
        assertThat(minca.activa(dentroDeUnMes)).isTrue();
        List<AlertaClimatica> ahora = adaptador(new ClassPathResource("alertas/alertas-simuladas.json"), AHORA)
                .consultar(List.of()).alertas();
        assertThat(porId(ahora, "simulada-taganga-oleaje").vigenteHasta()).as("sin vigentePor no tiene fin").isNull();
    }

    @Test
    @DisplayName("un archivo ilegible es una falla de la fuente")
    void ilegible() {
        ByteArrayResource roto = new ByteArrayResource("[{\"id\": ".getBytes(StandardCharsets.UTF_8));

        assertThatThrownBy(() -> adaptador(roto, AHORA).consultar(List.of()))
                .isInstanceOf(FuenteAlertasNoDisponibleException.class);
    }

    private AlertasSimuladasAdapter adaptador(org.springframework.core.io.Resource archivo, Instant ahora) {
        return new AlertasSimuladasAdapter(archivo, json, Clock.fixed(ahora, ZoneOffset.UTC));
    }

    private static AlertaClimatica porId(List<AlertaClimatica> alertas, String id) {
        return alertas.stream().filter(a -> a.id().equals(id)).findFirst().orElseThrow();
    }
}
