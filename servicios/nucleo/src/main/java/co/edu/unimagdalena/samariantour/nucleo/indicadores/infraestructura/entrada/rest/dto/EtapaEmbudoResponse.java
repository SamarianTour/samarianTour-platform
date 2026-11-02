package co.edu.unimagdalena.samariantour.nucleo.indicadores.infraestructura.entrada.rest.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.math.BigDecimal;

/**
 * @param etapa       ENVIADAS, ATENDIDAS, CONFIRMADAS o PAGADAS
 * @param solicitudes se omite si es 1 o 2 (RET-03)
 * @param porcentaje  sobre las enviadas; se omite con el conteo
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record EtapaEmbudoResponse(String etapa, Long solicitudes, BigDecimal porcentaje) {
}
