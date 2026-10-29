package co.edu.unimagdalena.samariantour.nucleo.pagos.infraestructura.salida.pasarela;

import co.edu.unimagdalena.samariantour.nucleo.pagos.dominio.excepcion.EventoNoAutenticoException;
import co.edu.unimagdalena.samariantour.nucleo.pagos.dominio.modelo.EstadoTransaccion;
import co.edu.unimagdalena.samariantour.nucleo.pagos.dominio.modelo.EventoVerificado;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Locale;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Las firmas de Wompi con los ejemplos de su documentación oficial (docs.wompi.co, octubre de 2026). Ninguna prueba
 * llama a Wompi: los secretos son los de los ejemplos publicados.
 */
class FormatoWompiTest {

    private static final ObjectMapper JSON = new ObjectMapper();
    /** El secreto de eventos del ejemplo de "Eventos" de Wompi. */
    private static final String SECRETO = "prod_events_OcHnIzeBl5socpwByQ4hA52Em3USQ93Z";
    /**
     * El evento de ejemplo de "Eventos". Su checksum publicado (3476DDA5...) no corresponde a su propia cadena, así que
     * aquí va el que sale de aplicar el algoritmo publicado a esos datos.
     */
    private static final String EVENTO = """
            {
              "event": "transaction.updated",
              "data": {
                "transaction": {
                  "id": "1234-1610641025-49201",
                  "amount_in_cents": 4490000,
                  "reference": "MZQ3X2DE2SMX",
                  "customer_email": "juan.perez@gmail.com",
                  "currency": "COP",
                  "payment_method_type": "NEQUI",
                  "redirect_url": "https://mitienda.com.co/pagos/redireccion",
                  "status": "APPROVED",
                  "shipping_address": null,
                  "payment_link_id": null,
                  "payment_source_id": null
                }
              },
              "environment": "prod",
              "signature": {
                "properties": ["transaction.id", "transaction.status", "transaction.amount_in_cents"],
                "checksum": "%s"
              },
              "timestamp": 1530291411,
              "sent_at": "2018-07-20T16:45:05.000Z"
            }""";
    private static final String CHECKSUM = "5a18ec5e8fdb7df463e9f94774cba8f583ba21bd04a09ceff2ea68a4bc0aefbe";

    @Test
    @DisplayName("firma de integridad: el ejemplo de \"Widget y Web Checkout\" da 37c84077... (RF-46)")
    void integridad() {
        assertThat(FormatoWompi.firmaIntegridad("sk8-438k4-xmxm392-sn2m", 2_490_000, "COP",
                "prod_integrity_Z5mMke9x0k8gpErbDqwrJXMqsI6SFli6"))
                .isEqualTo("37c8407747e595535433ef8f6a811d853cd943046624a0ec04662b17bbf33bf5");
    }

    @Test
    @DisplayName("checksum: los ejemplos de \"Eventos de Pagos a terceros\", con otras propiedades y otro timestamp")
    void checksumConOtrasPropiedades() throws Exception {
        String evento = """
                {"event": "transaction.updated",
                 "data": {"transaction": {"id": "04a6e53d-a244-4140-ab9e-48fa541f9fe5", "status": "FAILED",
                                          "amountInCents": 7500000}},
                 "signature": {"properties": ["transaction.id", "transaction.status", "transaction.amountInCents"]},
                 "timestamp": 1747673128600}""";

        assertThat(FormatoWompi.checksum(JSON.readTree(evento), "prod_events_7b193c8afd7b47949f90d443cb1e1742"))
                .isEqualTo("82f0e769716170e202edfd348f604bd8461cdeeb416594cde563a890215a5282");
        assertThat(FormatoWompi.sha256("04a6e53d-a244-4140-ab9e-48fa541f9fe5TOTAL_PAYMENT75000001747673128600"
                + "prod_events_7b193c8afd7b47949f90d443cb1e1742"))
                .isEqualTo("639dc6bd2ac0104f090651c07773b6537f935623cf0ed04894f0687d4c9eebc7");
    }

    @Test
    @DisplayName("el evento de ejemplo se verifica y trae la transacción, también con el checksum en mayúsculas")
    void verificar() {
        Optional<EventoVerificado> verificado = FormatoWompi.verificar(EVENTO.formatted(CHECKSUM), null, SECRETO, JSON);

        assertThat(verificado).hasValueSatisfying(evento -> {
            assertThat(evento.transaccionId()).isEqualTo("1234-1610641025-49201");
            assertThat(evento.referencia()).isEqualTo("MZQ3X2DE2SMX");
            assertThat(evento.estado()).isEqualTo(EstadoTransaccion.APROBADA);
            assertThat(evento.montoCentavos()).isEqualTo(4_490_000);
            assertThat(evento.claveUnica()).isEqualTo("1234-1610641025-49201:APROBADA");
        });
        assertThat(FormatoWompi.verificar(EVENTO.formatted(CHECKSUM.toUpperCase(Locale.ROOT)), CHECKSUM, SECRETO,
                JSON)).isPresent();
    }

    @Test
    @DisplayName("un evento alterado, con otro secreto, con otra firma en la cabecera o sin firma se rechaza (RF-50)")
    void rechazos() {
        String alterado = EVENTO.formatted(CHECKSUM).replace("4490000", "4490001");

        assertThatThrownBy(() -> FormatoWompi.verificar(alterado, null, SECRETO, JSON))
                .isInstanceOf(EventoNoAutenticoException.class).hasMessageContaining("firma no coincide");
        assertThatThrownBy(() -> FormatoWompi.verificar(EVENTO.formatted(CHECKSUM), null, "otro_secreto", JSON))
                .isInstanceOf(EventoNoAutenticoException.class);
        assertThatThrownBy(() -> FormatoWompi.verificar(EVENTO.formatted(CHECKSUM), "abc", SECRETO, JSON))
                .isInstanceOf(EventoNoAutenticoException.class);
        assertThatThrownBy(() -> FormatoWompi.verificar(EVENTO.formatted(""), null, SECRETO, JSON))
                .isInstanceOf(EventoNoAutenticoException.class).hasMessageContaining("firma");
        assertThatThrownBy(() -> FormatoWompi.verificar("no es json", null, SECRETO, JSON))
                .isInstanceOf(EventoNoAutenticoException.class);
        assertThatThrownBy(() -> FormatoWompi.verificar(null, null, SECRETO, JSON))
                .isInstanceOf(EventoNoAutenticoException.class);
    }

    @Test
    @DisplayName("un evento auténtico que no es de una transacción se acepta sin datos: 200 y no se guarda")
    void otroEvento() throws Exception {
        String evento = """
                {"event": "nequi_token.updated", "data": {"token": {"id": "nequi_1", "status": "APPROVED"}},
                 "signature": {"properties": ["token.id", "token.status"], "checksum": "%s"},
                 "timestamp": 1530291411}""";
        String checksum = FormatoWompi.checksum(JSON.readTree(evento.formatted("")), SECRETO);

        assertThat(FormatoWompi.verificar(evento.formatted(checksum), null, SECRETO, JSON)).isEmpty();
    }

    @Test
    @DisplayName("el evento que arma la pasarela simulada tiene la forma del de Wompi y se verifica igual")
    void eventoSimulado() {
        String evento = FormatoWompi.eventoFirmado("SIM-1", "ST-1", 30_000_000, "DECLINED", "test", 1_761_577_200L,
                "2026-10-27T15:00:00Z", "secreto-de-demo", JSON);

        assertThat(evento).contains("\"event\":\"transaction.updated\"", "\"reference\":\"ST-1\"",
                "\"status\":\"DECLINED\"", "\"properties\":[\"transaction.id\",\"transaction.status\","
                        + "\"transaction.amount_in_cents\"]");
        assertThat(FormatoWompi.verificar(evento, null, "secreto-de-demo", JSON)).hasValueSatisfying(verificado -> {
            assertThat(verificado.estado()).isEqualTo(EstadoTransaccion.RECHAZADA);
            assertThat(verificado.montoCentavos()).isEqualTo(30_000_000);
        });
        assertThatThrownBy(() -> FormatoWompi.verificar(evento, null, "otro", JSON))
                .isInstanceOf(EventoNoAutenticoException.class);
    }
}
