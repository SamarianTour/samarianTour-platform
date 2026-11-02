package co.edu.unimagdalena.samariantour.nucleo.indicadores.dominio.modelo;

import java.math.BigDecimal;

/**
 * @param solicitudes vacío si es 1 o 2 (RET-03)
 * @param porcentaje  sobre las enviadas; vacío con el conteo o si no hay enviadas
 */
public record EtapaEmbudo(Etapa etapa, Long solicitudes, BigDecimal porcentaje) {
}
