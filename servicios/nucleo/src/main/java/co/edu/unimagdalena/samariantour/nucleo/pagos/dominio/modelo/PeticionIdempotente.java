package co.edu.unimagdalena.samariantour.nucleo.pagos.dominio.modelo;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.HexFormat;
import java.util.Objects;
import java.util.UUID;

/**
 * Una operación del turista con su clave de idempotencia (ADR-04, DN-19), como la de las solicitudes (DN-12): la misma
 * clave con la misma petición devuelve la respuesta original sin repetir la operación.
 *
 * @param huella SHA-256 de la operación y de la petición, en hexadecimal: la misma clave con otra petición es un error
 */
public record PeticionIdempotente(UUID clave, UUID usuarioId, String operacion, String huella) {

    /** Lo mismo que en las solicitudes [SUPUESTO] (DN-12). */
    public static final Duration VIGENCIA = Duration.ofHours(24);

    public PeticionIdempotente {
        Objects.requireNonNull(clave, "clave");
        Objects.requireNonNull(usuarioId, "usuarioId");
        Objects.requireNonNull(operacion, "operacion");
        Objects.requireNonNull(huella, "huella");
    }

    /** @param peticion la petición en una forma canónica: el recurso y el cuerpo convertido de nuevo a JSON */
    public static PeticionIdempotente de(UUID clave, UUID usuarioId, String operacion, String peticion) {
        try {
            String huella = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest((operacion + "\n" + peticion).getBytes(StandardCharsets.UTF_8)));
            return new PeticionIdempotente(clave, usuarioId, operacion, huella);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("La JVM no tiene SHA-256", e);
        }
    }
}
