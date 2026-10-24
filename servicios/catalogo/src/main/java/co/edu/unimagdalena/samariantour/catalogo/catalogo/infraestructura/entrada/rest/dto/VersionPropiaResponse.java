package co.edu.unimagdalena.samariantour.catalogo.catalogo.infraestructura.entrada.rest.dto;

import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.EstadoModeracion;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;

/**
 * Una versión de una oferta propia con su contenido completo, el mismo que ve el administrador al moderarla (DC-50).
 *
 * @param motivoRechazo solo si el administrador la rechazó
 * @param enviadaEn     cuándo se envió a moderación
 * @param revisadaEn    cuándo se aprobó o se rechazó; vacío mientras está en moderación
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record VersionPropiaResponse(
        int numero,
        EstadoModeracion estadoModeracion,
        String motivoRechazo,
        Instant enviadaEn,
        Instant revisadaEn,
        ContenidoVersionResponse contenido) {
}
