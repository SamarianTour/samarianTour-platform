package co.edu.unimagdalena.samariantour.nucleo.pagos.dominio.modelo;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Un evento de la pasarela ya verificado, tal como llegó por el webhook (RF-50, DN-21). Se guarda al recibirlo y una
 * tarea programada lo procesa después.
 *
 * @param claveUnica    el id de la transacción y su estado [SUPUESTO]: Wompi no manda un id de evento y reintenta el
 *                      envío, así que el mismo par es el mismo evento
 * @param cuerpo        el JSON recibido, sin cambios
 * @param procesadoEn   vacío hasta procesarlo
 * @param resultado     qué hizo el procesamiento, para el seguimiento: APLICADO, SIN_CAMBIOS, PAGO_TARDIO,
 *                      SIN_INTENTO o el código del error
 */
public record EventoPasarela(UUID id, String claveUnica, String transaccionId, String referencia,
                             EstadoTransaccion estado, long montoCentavos, String cuerpo, Instant recibidoEn,
                             Instant procesadoEn, String resultado) {

    public EventoPasarela {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(claveUnica, "claveUnica");
        Objects.requireNonNull(transaccionId, "transaccionId");
        Objects.requireNonNull(referencia, "referencia");
        Objects.requireNonNull(estado, "estado");
        Objects.requireNonNull(cuerpo, "cuerpo");
        Objects.requireNonNull(recibidoEn, "recibidoEn");
    }

    public static EventoPasarela recibido(UUID id, EventoVerificado verificado, Instant ahora) {
        return new EventoPasarela(id, verificado.claveUnica(), verificado.transaccionId(), verificado.referencia(),
                verificado.estado(), verificado.montoCentavos(), verificado.cuerpo(), ahora, null, null);
    }

    public EventoPasarela procesado(String resultado, Instant ahora) {
        return new EventoPasarela(id, claveUnica, transaccionId, referencia, estado, montoCentavos, cuerpo, recibidoEn,
                ahora, resultado);
    }
}
