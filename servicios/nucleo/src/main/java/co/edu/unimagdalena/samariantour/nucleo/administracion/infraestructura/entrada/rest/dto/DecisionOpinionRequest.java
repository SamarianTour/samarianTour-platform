package co.edu.unimagdalena.samariantour.nucleo.administracion.infraestructura.entrada.rest.dto;

/**
 * @param decision APROBAR o RECHAZAR
 * @param motivo   obligatorio al rechazar
 */
public record DecisionOpinionRequest(String decision, String motivo) {
}
