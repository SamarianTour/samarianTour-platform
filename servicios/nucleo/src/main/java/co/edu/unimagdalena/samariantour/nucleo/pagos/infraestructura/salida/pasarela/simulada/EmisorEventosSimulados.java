package co.edu.unimagdalena.samariantour.nucleo.pagos.infraestructura.salida.pasarela.simulada;

import co.edu.unimagdalena.samariantour.nucleo.pagos.infraestructura.config.PropiedadesPagos;
import co.edu.unimagdalena.samariantour.nucleo.pagos.infraestructura.salida.pasarela.FormatoWompi;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.http.MediaType;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

/**
 * Lo que hace la pasarela simulada cuando el turista pulsa un botón de su página (DN-18): unos segundos después arma un
 * evento con la forma del de Wompi, lo firma con el secreto de demo y lo envía al webhook del núcleo, igual que una
 * pasarela real. El resultado nunca llega por la página: llega por el webhook (RF-50, RF-51).
 */
@Component
@Profile("demo")
@ConditionalOnProperty(name = "nucleo.pagos.pasarela", havingValue = "simulada", matchIfMissing = true)
public class EmisorEventosSimulados {

    private static final Logger LOG = LoggerFactory.getLogger(EmisorEventosSimulados.class);

    private final PropiedadesPagos propiedades;
    private final TaskScheduler tareas;
    private final RestClient http;
    private final ObjectMapper json;
    private final Clock reloj;

    public EmisorEventosSimulados(PropiedadesPagos propiedades, TaskScheduler tareas, ObjectMapper json, Clock reloj) {
        this.propiedades = propiedades;
        this.tareas = tareas;
        this.http = RestClient.create();
        this.json = json;
        this.reloj = reloj;
    }

    /**
     * Programa el envío del evento.
     *
     * @param estadoWompi APPROVED o DECLINED
     * @return el id de la transacción simulada, el mismo que traerá el evento
     */
    public String programar(String referencia, long montoCentavos, String estadoWompi) {
        String transaccionId = "SIM-" + UUID.randomUUID();
        tareas.schedule(() -> enviar(transaccionId, referencia, montoCentavos, estadoWompi),
                reloj.instant().plus(propiedades.simulada().demoraEvento()));
        return transaccionId;
    }

    private void enviar(String transaccionId, String referencia, long montoCentavos, String estadoWompi) {
        Instant ahora = reloj.instant();
        String evento = FormatoWompi.eventoFirmado(transaccionId, referencia, montoCentavos, estadoWompi, "test",
                ahora.getEpochSecond(), ahora.toString(), propiedades.simulada().secretoEventos(), json);
        try {
            http.post().uri(propiedades.simulada().urlWebhook()).contentType(MediaType.APPLICATION_JSON).body(evento)
                    .retrieve().toBodilessEntity();
            LOG.info("[Pasarela simulada] evento {} enviado: referencia={} transaccion={}", estadoWompi, referencia,
                    transaccionId);
        } catch (RestClientException e) {
            LOG.warn("[Pasarela simulada] el webhook no recibió el evento de {}: {}", referencia,
                    e.getClass().getSimpleName());
        }
    }
}
