package co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.modelo;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Objects;
import java.util.UUID;

/**
 * Un cambio de estado de la solicitud, para el historial que ve el turista (RF-33) y para los reportes (RF-40, RF-88).
 *
 * @param de      vacío en la creación
 * @param actorId quién lo hizo; vacío si fue automático o de la pasarela
 * @param motivo  la razón, cuando aplica; nunca el detalle de la negociación con el prestador (RF-40)
 */
public record TransicionSolicitud(EstadoSolicitud de, EstadoSolicitud a, Instant en, ActorTransicion actor,
                                  UUID actorId, String motivo) {

    public TransicionSolicitud {
        Objects.requireNonNull(a, "a");
        Objects.requireNonNull(actor, "actor");
        // Con la precisión de PostgreSQL: la transición recién creada y la leída después tienen la misma fecha.
        en = Objects.requireNonNull(en, "en").truncatedTo(ChronoUnit.MICROS);
    }
}
