package co.edu.unimagdalena.samariantour.recomendacion.recomendacion.infraestructura.entrada.rest.dto;

import java.time.Instant;
import java.util.List;

/**
 * Esquema {@code ResumenAlertas}: todas las alertas activas agrupadas por zona, para la vista general.
 *
 * @param zonas solo las que tienen al menos una alerta activa
 */
public record ResumenAlertasResponse(Instant actualizadoEn, List<ZonaConAlertasResponse> zonas) {
}
