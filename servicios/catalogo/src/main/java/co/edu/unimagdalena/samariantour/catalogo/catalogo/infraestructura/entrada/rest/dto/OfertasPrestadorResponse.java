package co.edu.unimagdalena.samariantour.catalogo.catalogo.infraestructura.entrada.rest.dto;

import java.util.UUID;

/** @param ofertas las ofertas visibles del prestador (DC-51) */
public record OfertasPrestadorResponse(UUID prestadorId, long ofertas) {
}
