package co.edu.unimagdalena.samariantour.nucleo.indicadores.infraestructura.entrada.rest.dto;

import java.util.UUID;

/** @param posicion desde 1 */
public record OfertaExpuestaRequest(String ofertaId, UUID prestadorId, Integer posicion) {
}
