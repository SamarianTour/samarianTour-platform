package co.edu.unimagdalena.samariantour.catalogo.opiniones.infraestructura.entrada.rest.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;

/**
 * Una opinión que espera la decisión del administrador (RF-80, DC-43), con el nombre de la oferta para dar contexto.
 * No lleva el turista: el administrador no lo necesita para decidir, y el núcleo lo consulta por la ruta interna.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record OpinionPendienteResponse(
        String id,
        String ofertaId,
        String nombreOferta,
        int calificacion,
        String comentario,
        Instant fecha) {
}
