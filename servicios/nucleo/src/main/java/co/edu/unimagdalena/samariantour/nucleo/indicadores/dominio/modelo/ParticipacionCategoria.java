package co.edu.unimagdalena.samariantour.nucleo.indicadores.dominio.modelo;

import java.math.BigDecimal;

/**
 * @param solicitudes vacío si es 1 o 2 (RET-03)
 * @param porcentaje  sobre el total de solicitudes; vacío con el conteo
 * @param pagadas     vacío si es 1 o 2
 */
public record ParticipacionCategoria(String categoria, Long solicitudes, BigDecimal porcentaje, Long pagadas) {
}
