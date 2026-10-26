package co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.modelo;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.HexFormat;
import java.util.Objects;
import java.util.UUID;

/**
 * Una operación del turista que llega con su clave de idempotencia (ADR-04, DN-12). El cliente genera la clave antes
 * del primer intento y la repite en cada reintento: la misma clave con la misma petición devuelve la respuesta
 * original sin repetir la operación.
 *
 * @param operacion qué operación es, por ejemplo {@code ENVIAR_SOLICITUD}
 * @param huella    SHA-256 de la operación y de la petición, en hexadecimal: la misma clave con otra petición es un
 *                  error
 */
public record PeticionIdempotente(UUID clave, UUID usuarioId, String operacion, String huella) {

    /** Cuánto dura una clave [SUPUESTO] (DN-12): después, la misma clave cuenta como nueva. */
    public static final Duration VIGENCIA = Duration.ofHours(24);

    public PeticionIdempotente {
        Objects.requireNonNull(clave, "clave");
        Objects.requireNonNull(usuarioId, "usuarioId");
        Objects.requireNonNull(operacion, "operacion");
        Objects.requireNonNull(huella, "huella");
    }

    /**
     * @param peticion la petición en una forma canónica: dos peticiones equivalentes deben dar el mismo texto (por
     *                 ejemplo, el recurso y el cuerpo convertido de nuevo a JSON, sin depender de espacios ni orden)
     */
    public static PeticionIdempotente de(UUID clave, UUID usuarioId, String operacion, String peticion) {
        return new PeticionIdempotente(clave, usuarioId, operacion, sha256(operacion + "\n" + peticion));
    }

    private static String sha256(String texto) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(texto.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("La JVM no tiene SHA-256", e);
        }
    }
}
