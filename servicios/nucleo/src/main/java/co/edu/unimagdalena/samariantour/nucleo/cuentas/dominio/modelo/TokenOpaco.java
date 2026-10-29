package co.edu.unimagdalena.samariantour.nucleo.cuentas.dominio.modelo;

import java.util.Base64;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.random.RandomGenerator;

/**
 * Un secreto de un solo uso que recibe el cliente, con la forma {@code <id>.<secreto>} (DN-19, DN-20): el token de
 * renovación de una {@link Sesion} y el enlace para restablecer la contraseña de un {@link CodigoUnUso}. El
 * identificador encuentra lo guardado y el secreto se compara con su hash; el secreto nunca se guarda.
 */
public record TokenOpaco(UUID id, String secreto) {

    private static final int BYTES_SECRETO = 32;

    public TokenOpaco {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(secreto, "secreto");
    }

    /** Un secreto nuevo de 32 bytes aleatorios. */
    public static TokenOpaco nuevo(UUID id, RandomGenerator azar) {
        byte[] secreto = new byte[BYTES_SECRETO];
        azar.nextBytes(secreto);
        return new TokenOpaco(id, Base64.getUrlEncoder().withoutPadding().encodeToString(secreto));
    }

    /** @return vacío si el texto no tiene la forma {@code <id>.<secreto>} */
    public static Optional<TokenOpaco> leer(String valor) {
        if (valor == null) {
            return Optional.empty();
        }
        int punto = valor.indexOf('.');
        if (punto < 1 || punto == valor.length() - 1) {
            return Optional.empty();
        }
        try {
            return Optional.of(new TokenOpaco(UUID.fromString(valor.substring(0, punto)),
                    valor.substring(punto + 1)));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    public String valor() {
        return id + "." + secreto;
    }

    @Override
    public String toString() {
        return "TokenOpaco[id=%s, secreto=******]".formatted(id);
    }
}
