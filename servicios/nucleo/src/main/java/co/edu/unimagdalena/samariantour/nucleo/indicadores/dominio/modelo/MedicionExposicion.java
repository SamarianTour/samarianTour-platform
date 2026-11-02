package co.edu.unimagdalena.samariantour.nucleo.indicadores.dominio.modelo;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * El índice de exposición de un prestador (RET-02): su porcentaje de las apariciones dividido por su porcentaje de las
 * ofertas elegibles. 1 es justo; más de 1,5, sospechoso ({@code IndiceExposicion}).
 */
public record MedicionExposicion(UUID prestadorId, long apariciones, BigDecimal porcentajeApariciones,
                                 BigDecimal porcentajeOfertas, BigDecimal indice) {
}
