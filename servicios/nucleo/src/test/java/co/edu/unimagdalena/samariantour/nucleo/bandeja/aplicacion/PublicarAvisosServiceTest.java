package co.edu.unimagdalena.samariantour.nucleo.bandeja.aplicacion;

import co.edu.unimagdalena.samariantour.nucleo.bandeja.dominio.modelo.AvisoSaliente;
import co.edu.unimagdalena.samariantour.nucleo.bandeja.dominio.modelo.Destino;
import co.edu.unimagdalena.samariantour.nucleo.bandeja.dominio.modelo.EstadoAviso;
import co.edu.unimagdalena.samariantour.nucleo.bandeja.dominio.modelo.ResultadoEnvio;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/** La publicación de la bandeja de salida con el catálogo simulado (DN-06). */
class PublicarAvisosServiceTest {

    private static final Instant AHORA = Instant.parse("2026-10-24T15:00:00Z");

    private final BandejaEnMemoria bandeja = new BandejaEnMemoria();
    private final List<String> enviados = new ArrayList<>();

    @Test
    @DisplayName("envía cada aviso pendiente a su destino y registra lo que respondió el catálogo")
    void registraCadaResultado() {
        encolar("recibido", Destino.OPINIONES);
        encolar("rechazado", Destino.OFERTAS);
        encolar("caido", Destino.OFERTAS);
        PublicarAvisosService servicio = servicio(Map.of(
                "recibido", new ResultadoEnvio.Recibido(),
                "rechazado", new ResultadoEnvio.Rechazado("400"),
                "caido", new ResultadoEnvio.Fallido("timeout")), AHORA);

        assertThat(servicio.publicarLote()).isEqualTo(3);

        assertThat(enviados).containsExactly("OPINIONES recibido", "OFERTAS rechazado", "OFERTAS caido");
        assertThat(estado("recibido")).isEqualTo(EstadoAviso.ENVIADO);
        assertThat(estado("rechazado")).isEqualTo(EstadoAviso.RECHAZADO);
        assertThat(estado("caido")).isEqualTo(EstadoAviso.PENDIENTE);
        assertThat(bandeja.buscar("caido").orElseThrow().proximoIntento()).isEqualTo(AHORA.plusSeconds(5));
    }

    @Test
    @DisplayName("un aviso fallido no se reintenta antes de su espera; uno enviado o rechazado, nunca")
    void respetaLaEspera() {
        encolar("recibido", Destino.OFERTAS);
        encolar("caido", Destino.OFERTAS);
        Map<String, ResultadoEnvio> respuestas = Map.of(
                "recibido", new ResultadoEnvio.Recibido(), "caido", new ResultadoEnvio.Fallido("503"));
        servicio(respuestas, AHORA).publicarLote();
        enviados.clear();

        assertThat(servicio(respuestas, AHORA.plusSeconds(4)).publicarLote()).isZero();
        assertThat(servicio(respuestas, AHORA.plusSeconds(5)).publicarLote()).isEqualTo(1);
        assertThat(enviados).containsExactly("OFERTAS caido");
        assertThat(bandeja.buscar("caido").orElseThrow().intentos()).isEqualTo(2);
    }

    @Test
    @DisplayName("toma como máximo un lote por llamada, primero los que llevan más tiempo esperando")
    void loteLimitado() {
        for (int i = 1; i <= PublicarAvisosService.TAMANO_LOTE + 2; i++) {
            String id = "a-%02d".formatted(i);
            bandeja.agregar(AvisoSaliente.nuevo(id, Destino.OFERTAS, cuerpo(id), AHORA.plusMillis(i)));
        }
        PublicarAvisosService servicio = servicio(Map.of(), AHORA.plusSeconds(1));

        assertThat(servicio.publicarLote()).isEqualTo(PublicarAvisosService.TAMANO_LOTE);
        assertThat(enviados).first().isEqualTo("OFERTAS a-01");
        assertThat(servicio.publicarLote()).isEqualTo(2);
    }

    private void encolar(String id, Destino destino) {
        bandeja.agregar(AvisoSaliente.nuevo(id, destino, cuerpo(id), AHORA));
    }

    private static String cuerpo(String id) {
        return "{\"idAviso\":\"" + id + "\"}";
    }

    private EstadoAviso estado(String id) {
        return bandeja.buscar(id).orElseThrow().estado();
    }

    /** El catálogo simulado responde según el idAviso del cuerpo; si no lo conoce, lo recibe. */
    private PublicarAvisosService servicio(Map<String, ResultadoEnvio> respuestas, Instant ahora) {
        return new PublicarAvisosService(bandeja, (destino, cuerpo) -> {
            String id = cuerpo.substring("{\"idAviso\":\"".length(), cuerpo.length() - 2);
            enviados.add(destino + " " + id);
            return respuestas.getOrDefault(id, new ResultadoEnvio.Recibido());
        }, Clock.fixed(ahora, ZoneOffset.UTC));
    }
}
