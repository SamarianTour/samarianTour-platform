package co.edu.unimagdalena.samariantour.recomendacion.recomendacion.infraestructura.entrada.rest.dto;

import co.edu.unimagdalena.samariantour.recomendacion.recomendacion.dominio.modelo.CriterioAfinidad;

import java.util.List;

/**
 * Esquema {@code Recomendacion}: la oferta, su posición y la explicación del criterio usado (RF-27).
 */
public record RecomendacionResponse(int posicion, OfertaRecomendadaResponse oferta, List<CriterioAfinidad> criterios,
                                    String explicacion) {
}
