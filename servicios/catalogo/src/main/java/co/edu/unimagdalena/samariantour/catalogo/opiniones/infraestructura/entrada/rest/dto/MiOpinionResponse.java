package co.edu.unimagdalena.samariantour.catalogo.opiniones.infraestructura.entrada.rest.dto;

import co.edu.unimagdalena.samariantour.catalogo.opiniones.dominio.modelo.EstadoOpinion;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;

/**
 * Una opinión como la ve su autor, en cualquier estado.
 *
 * @param nombreOferta  el de la oferta en el catálogo, para dar contexto
 * @param motivoRechazo solo si fue rechazada
 * @param revisadaEn    solo si ya se moderó
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record MiOpinionResponse(
        String id,
        String ofertaId,
        String nombreOferta,
        int calificacion,
        String comentario,
        Instant fecha,
        EstadoOpinion estado,
        String motivoRechazo,
        Instant revisadaEn) {
}
