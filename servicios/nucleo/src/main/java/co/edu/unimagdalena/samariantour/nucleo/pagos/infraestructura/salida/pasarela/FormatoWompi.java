package co.edu.unimagdalena.samariantour.nucleo.pagos.infraestructura.salida.pasarela;

import co.edu.unimagdalena.samariantour.nucleo.pagos.dominio.excepcion.EventoNoAutenticoException;
import co.edu.unimagdalena.samariantour.nucleo.pagos.dominio.modelo.EstadoTransaccion;
import co.edu.unimagdalena.samariantour.nucleo.pagos.dominio.modelo.EventoVerificado;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * El formato y las firmas de Wompi (documentación oficial, "Eventos" y "Widget y Web Checkout", DN-21). Lo usan los dos
 * adaptadores: Wompi, con los secretos de la cuenta, y la simulada, que arma eventos con la misma forma y los firma con
 * un secreto de demo, así la demo recorre el mismo camino que un pago real.
 * <ul>
 *   <li><b>Firma de integridad</b> del checkout: SHA-256, en hexadecimal, de la referencia, el monto en centavos, la
 *   moneda y el secreto de integridad, concatenados en ese orden.</li>
 *   <li><b>Checksum del evento</b>: SHA-256 de los valores de {@code signature.properties} (rutas dentro de
 *   {@code data}, en su orden y leídas de cada evento, porque pueden cambiar), el {@code timestamp} y el secreto de
 *   eventos, concatenados. Se compara sin distinguir mayúsculas: el ejemplo de la documentación viene en mayúsculas y
 *   sus fragmentos de código producen minúsculas.</li>
 * </ul>
 */
public final class FormatoWompi {

    public static final String EVENTO_TRANSACCION = "transaction.updated";
    static final List<String> PROPIEDADES_FIRMADAS = List.of("transaction.id", "transaction.status",
            "transaction.amount_in_cents");

    private FormatoWompi() {
    }

    /** La firma de integridad de una sesión de cobro. */
    public static String firmaIntegridad(String referencia, long montoCentavos, String moneda, String secreto) {
        return sha256(referencia + montoCentavos + moneda + secreto);
    }

    /**
     * Verifica el checksum y lee la transacción del evento.
     *
     * @param firmaCabecera la de {@code X-Event-Checksum}, si llegó; si llegó, también debe coincidir
     * @return vacío si el evento es auténtico pero no es de una transacción
     * @throws EventoNoAutenticoException si no se puede leer o la firma no coincide
     */
    public static Optional<EventoVerificado> verificar(String cuerpo, String firmaCabecera, String secretoEventos,
                                                      ObjectMapper json) {
        JsonNode evento = leer(cuerpo, json);
        JsonNode firma = evento.path("signature");
        String recibida = firma.path("checksum").asText("");
        if (recibida.isBlank() || !firma.path("properties").isArray() || !evento.hasNonNull("timestamp")) {
            throw new EventoNoAutenticoException("le falta la firma");
        }
        String esperada = checksum(evento, secretoEventos);
        if (!iguales(esperada, recibida) || (firmaCabecera != null && !firmaCabecera.isBlank()
                && !iguales(esperada, firmaCabecera))) {
            throw new EventoNoAutenticoException("la firma no coincide");
        }
        if (!EVENTO_TRANSACCION.equals(evento.path("event").asText())) {
            return Optional.empty();
        }
        JsonNode transaccion = evento.path("data").path("transaction");
        Optional<EstadoTransaccion> estado = estado(transaccion.path("status").asText());
        if (estado.isEmpty() || !transaccion.hasNonNull("id") || !transaccion.hasNonNull("reference")
                || !transaccion.path("amount_in_cents").canConvertToLong()) {
            throw new EventoNoAutenticoException("la transacción no trae id, referencia, estado o monto");
        }
        return Optional.of(new EventoVerificado(transaccion.path("id").asText(), transaccion.path("reference").asText(),
                estado.get(), transaccion.path("amount_in_cents").asLong(), cuerpo));
    }

    /** El checksum que corresponde a un evento con su secreto. */
    static String checksum(JsonNode evento, String secreto) {
        StringBuilder concatenado = new StringBuilder();
        for (JsonNode propiedad : evento.path("signature").path("properties")) {
            JsonNode valor = evento.path("data");
            for (String parte : propiedad.asText().split("\\.")) {
                valor = valor.path(parte);
            }
            concatenado.append(valor.asText());
        }
        concatenado.append(evento.path("timestamp").asText()).append(secreto);
        return sha256(concatenado.toString());
    }

    /**
     * Arma un evento {@code transaction.updated} con la forma del de Wompi y lo firma: lo usa la pasarela simulada.
     *
     * @param estadoWompi APPROVED, DECLINED, VOIDED, ERROR o PENDING
     */
    public static String eventoFirmado(String transaccionId, String referencia, long montoCentavos, String estadoWompi,
                                       String ambiente, long timestamp, String enviadoEn, String secreto,
                                       ObjectMapper json) {
        ObjectNode evento = json.createObjectNode();
        evento.put("event", EVENTO_TRANSACCION);
        ObjectNode transaccion = evento.putObject("data").putObject("transaction");
        transaccion.put("id", transaccionId);
        transaccion.put("amount_in_cents", montoCentavos);
        transaccion.put("reference", referencia);
        transaccion.put("currency", "COP");
        transaccion.put("payment_method_type", "CARD");
        transaccion.put("status", estadoWompi);
        evento.put("environment", ambiente);
        ObjectNode firma = evento.putObject("signature");
        PROPIEDADES_FIRMADAS.forEach(firma.putArray("properties")::add);
        evento.put("timestamp", timestamp);
        evento.put("sent_at", enviadoEn);
        firma.put("checksum", checksum(evento, secreto));
        try {
            return json.writeValueAsString(evento);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("No se pudo armar el evento", e);
        }
    }

    private static Optional<EstadoTransaccion> estado(String estadoWompi) {
        return Optional.ofNullable(switch (estadoWompi) {
            case "APPROVED" -> EstadoTransaccion.APROBADA;
            case "DECLINED" -> EstadoTransaccion.RECHAZADA;
            case "VOIDED" -> EstadoTransaccion.ANULADA;
            case "ERROR" -> EstadoTransaccion.ERROR;
            case "PENDING" -> EstadoTransaccion.PENDIENTE;
            default -> null;
        });
    }

    private static JsonNode leer(String cuerpo, ObjectMapper json) {
        try {
            JsonNode evento = cuerpo == null ? null : json.readTree(cuerpo);
            if (evento == null || !evento.isObject()) {
                throw new EventoNoAutenticoException("no es un JSON");
            }
            return evento;
        } catch (JsonProcessingException e) {
            throw new EventoNoAutenticoException("no es un JSON");
        }
    }

    /** Sin distinguir mayúsculas y en tiempo constante, para no dar pistas de la firma por la demora. */
    private static boolean iguales(String esperada, String recibida) {
        return MessageDigest.isEqual(esperada.toLowerCase(Locale.ROOT).getBytes(StandardCharsets.US_ASCII),
                recibida.strip().toLowerCase(Locale.ROOT).getBytes(StandardCharsets.US_ASCII));
    }

    static String sha256(String texto) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(texto.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("La JVM no tiene SHA-256", e);
        }
    }
}
