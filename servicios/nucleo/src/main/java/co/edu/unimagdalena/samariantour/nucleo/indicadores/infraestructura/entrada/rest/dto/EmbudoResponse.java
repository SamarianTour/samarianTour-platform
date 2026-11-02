package co.edu.unimagdalena.samariantour.nucleo.indicadores.infraestructura.entrada.rest.dto;

import java.time.LocalDate;
import java.util.List;

/** El embudo en total y el de cada tipo de oferta (RF-87, RF-88). */
public record EmbudoResponse(LocalDate desde, LocalDate hasta, List<EtapaEmbudoResponse> etapas,
                             List<EmbudoTipoResponse> porTipo) {
}
