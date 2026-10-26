package co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.puerto.entrada.comando;

import co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.excepcion.DatoInvalidoException;
import co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.modelo.DecisionTurista;

import java.util.Objects;
import java.util.UUID;

/**
 * La respuesta del turista a las condiciones de una solicitud atendida (RF-43).
 *
 * @param turistaId el de la identidad, nunca un campo del cuerpo
 */
public record ResponderCondicionesCommand(UUID turistaId, UUID solicitudId, DecisionTurista decision) {

    public ResponderCondicionesCommand {
        Objects.requireNonNull(turistaId, "turistaId");
        Objects.requireNonNull(solicitudId, "solicitudId");
        if (decision == null) {
            throw new DatoInvalidoException("decision", "debe ser CONFIRMAR o DECLINAR");
        }
    }
}
