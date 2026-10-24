package co.edu.unimagdalena.samariantour.nucleo.administracion.infraestructura.entrada.rest.dto;

/**
 * @param numeroVersion la versión que el administrador revisó en la cola del catálogo
 * @param decision      APROBAR o RECHAZAR
 * @param motivo        obligatorio al rechazar
 */
public record DecisionOfertaRequest(Integer numeroVersion, String decision, String motivo) {
}
