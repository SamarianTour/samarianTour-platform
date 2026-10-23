package co.edu.unimagdalena.samariantour.catalogo.catalogo.infraestructura.entrada.rest.dto;

import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.TipoOferta;

import java.time.Instant;
import java.util.UUID;

/**
 * Lo que el núcleo necesita de una oferta en moderación para decidir (DC-38): el prestador, para aplicar sus reglas
 * de aprobación y notificar el rechazo (RF-79), el nombre de la versión en moderación, para el mensaje, y su número,
 * que es el que nombra la decisión (DC-05).
 */
public record OfertaEnModeracionResponse(
        String id,
        TipoOferta tipo,
        String nombre,
        UUID prestadorId,
        int numeroVersion,
        Instant enviadaEn) {
}
