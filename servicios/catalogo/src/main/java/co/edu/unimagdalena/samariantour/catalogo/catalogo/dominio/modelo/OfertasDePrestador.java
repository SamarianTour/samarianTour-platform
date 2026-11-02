package co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo;

import java.util.UUID;

/** Cuántas ofertas visibles tiene un prestador, para el índice de exposición del núcleo (RET-02, DC-51). */
public record OfertasDePrestador(UUID prestadorId, long ofertas) {
}
