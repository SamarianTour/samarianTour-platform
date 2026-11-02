package co.edu.unimagdalena.samariantour.nucleo.indicadores.infraestructura.entrada.rest.dto;

import java.util.List;

public record EmbudoTipoResponse(String tipoOferta, List<EtapaEmbudoResponse> etapas) {
}
