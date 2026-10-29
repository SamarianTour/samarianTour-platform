package co.edu.unimagdalena.samariantour.nucleo.prestadores.dominio.modelo;

import java.time.Instant;
import java.util.Objects;

/** El código pendiente para verificar el WhatsApp: su hash, cuándo se emitió y los intentos fallidos (DN-33). */
public record CodigoWhatsapp(String hash, Instant emitidoEn, int intentosFallidos) {

    public CodigoWhatsapp {
        Objects.requireNonNull(hash, "hash");
        Objects.requireNonNull(emitidoEn, "emitidoEn");
    }
}
