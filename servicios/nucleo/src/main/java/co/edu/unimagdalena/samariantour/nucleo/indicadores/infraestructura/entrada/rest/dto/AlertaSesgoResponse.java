package co.edu.unimagdalena.samariantour.nucleo.indicadores.infraestructura.entrada.rest.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Una alerta de sesgo abierta (DN-43).
 *
 * @param indice          el porcentaje de apariciones dividido por el porcentaje de ofertas
 * @param recomendaciones las de los 30 días con las que se midió
 */
public record AlertaSesgoResponse(UUID prestadorId, BigDecimal indice, BigDecimal porcentajeApariciones,
                                  BigDecimal porcentajeOfertas, long apariciones, long recomendaciones,
                                  Instant abiertaEn, Instant actualizadaEn) {
}
