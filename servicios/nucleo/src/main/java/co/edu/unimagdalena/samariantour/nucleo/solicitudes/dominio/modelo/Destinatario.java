package co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.modelo;

import java.util.Objects;
import java.util.UUID;

/**
 * A quién va una notificación. Se guarda el identificador, no el número: el WhatsApp se busca al enviarla, así una
 * notificación pendiente no copia datos de contacto.
 */
public record Destinatario(Tipo tipo, UUID id) {

    public enum Tipo {
        TURISTA,
        PRESTADOR,
        OPERADOR
    }

    public Destinatario {
        Objects.requireNonNull(tipo, "tipo");
        Objects.requireNonNull(id, "id");
    }

    public static Destinatario turista(UUID id) {
        return new Destinatario(Tipo.TURISTA, id);
    }

    public static Destinatario prestador(UUID id) {
        return new Destinatario(Tipo.PRESTADOR, id);
    }

    public static Destinatario operador(UUID id) {
        return new Destinatario(Tipo.OPERADOR, id);
    }
}
