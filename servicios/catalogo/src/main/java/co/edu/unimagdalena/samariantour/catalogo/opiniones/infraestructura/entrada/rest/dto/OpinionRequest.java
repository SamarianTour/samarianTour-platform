package co.edu.unimagdalena.samariantour.catalogo.opiniones.infraestructura.entrada.rest.dto;

/**
 * La opinión que envía el turista (RF-60). El turista no va en el cuerpo: es el de la identidad de la petición.
 *
 * @param calificacion de 1 a 5
 * @param comentario   de 10 a 1000 caracteres sin los espacios de los extremos (DC-42)
 */
public record OpinionRequest(String ofertaId, Integer calificacion, String comentario) {
}
