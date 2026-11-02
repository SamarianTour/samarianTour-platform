package co.edu.unimagdalena.samariantour.nucleo.indicadores.infraestructura.entrada.rest.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDate;
import java.util.List;

/** @param total se omite si es 1 o 2 (RET-03) */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record TableroResponse(LocalDate desde, LocalDate hasta, Long total, List<DemandaZonaResponse> filas) {
}
