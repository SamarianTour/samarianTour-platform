package co.edu.unimagdalena.samariantour.catalogo.opiniones.infraestructura.entrada.rest.dto;

import co.edu.unimagdalena.samariantour.catalogo.opiniones.dominio.modelo.EstadoOpinion;

import java.time.Instant;
import java.util.UUID;

/**
 * Lo que el núcleo necesita de una opinión antes de decidir (DC-44): el turista, para notificarle la decisión, y la
 * oferta.
 */
public record OpinionEnModeracionResponse(
        String id,
        String ofertaId,
        UUID turistaId,
        EstadoOpinion estado,
        Instant fecha) {
}
