package co.edu.unimagdalena.samariantour.nucleo.administracion.dominio.modelo;

import java.util.UUID;

/**
 * Lo que el catálogo dice de una oferta en moderación (DC-38 del catálogo): su versión en moderación y a quién
 * notificar.
 *
 * @param nombre nombre de la versión en moderación, para el mensaje al prestador
 */
public record OfertaEnModeracion(String ofertaId, String nombre, UUID prestadorId, int numeroVersion) {
}
