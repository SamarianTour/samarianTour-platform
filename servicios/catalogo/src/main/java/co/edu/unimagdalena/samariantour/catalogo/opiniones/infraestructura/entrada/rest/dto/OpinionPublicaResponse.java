package co.edu.unimagdalena.samariantour.catalogo.opiniones.infraestructura.entrada.rest.dto;

import java.time.Instant;

/**
 * Una opinión aprobada como la lee cualquier turista (RF-59). No lleva el turista que la escribió: el catálogo no
 * tiene su nombre y su id no le sirve a nadie más (DC-43).
 */
public record OpinionPublicaResponse(String id, int calificacion, String comentario, Instant fecha) {
}
