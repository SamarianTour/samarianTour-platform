package co.edu.unimagdalena.samariantour.nucleo.indicadores.infraestructura.entrada.rest.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

/** @param solicitudes se omite si es 1 o 2: la pantalla muestra "menos de 3" (RET-03) */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record DemandaZonaResponse(String zonaId, String tipoOferta, Long solicitudes) {
}
