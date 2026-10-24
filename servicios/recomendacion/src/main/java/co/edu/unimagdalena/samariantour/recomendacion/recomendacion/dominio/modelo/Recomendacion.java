package co.edu.unimagdalena.samariantour.recomendacion.recomendacion.dominio.modelo;

/**
 * Una oferta recomendada, con su posición (desde 1) y su explicación.
 */
public record Recomendacion(int posicion, OfertaCandidata oferta, Explicacion explicacion) {
}
