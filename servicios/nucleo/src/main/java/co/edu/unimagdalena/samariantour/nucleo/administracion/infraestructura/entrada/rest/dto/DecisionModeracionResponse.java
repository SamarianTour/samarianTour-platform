package co.edu.unimagdalena.samariantour.nucleo.administracion.infraestructura.entrada.rest.dto;

import co.edu.unimagdalena.samariantour.nucleo.administracion.dominio.modelo.Decision;
import co.edu.unimagdalena.samariantour.nucleo.administracion.dominio.modelo.TipoModerado;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.UUID;

/**
 * La decisión guardada (DN-08). Si se repitió una decisión ya tomada, es la de entonces, con su fecha y su
 * administrador.
 *
 * @param numeroVersion solo para una oferta
 * @param motivo        solo si se rechazó
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record DecisionModeracionResponse(
        TipoModerado tipo,
        String moderadoId,
        Integer numeroVersion,
        Decision decision,
        String motivo,
        UUID administradorId,
        Instant decididaEn) {
}
