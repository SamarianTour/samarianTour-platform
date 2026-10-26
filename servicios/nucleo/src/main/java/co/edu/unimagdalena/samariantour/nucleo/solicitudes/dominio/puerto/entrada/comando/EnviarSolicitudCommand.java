package co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.puerto.entrada.comando;

import co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.excepcion.DatoInvalidoException;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Objects;
import java.util.UUID;

/**
 * Lo que envía el turista (RF-30). El turista es el de la identidad, nunca un campo del cuerpo; las demás reglas las
 * aplica la solicitud al crearse, incluidos los campos propios del tipo de oferta.
 *
 * @param horaSalida     solo en actividades y paquetes
 * @param horaReserva    solo en restaurantes
 * @param fechaSalida    solo en alojamientos
 * @param numeroUnidades solo en alojamientos
 */
public record EnviarSolicitudCommand(UUID turistaId, String ofertaId, LocalDate fechaInicio, LocalTime horaSalida,
                                     LocalTime horaReserva, LocalDate fechaSalida, Integer numeroUnidades,
                                     Integer personas, String nota) {

    public EnviarSolicitudCommand {
        Objects.requireNonNull(turistaId, "turistaId");
        if (ofertaId == null || ofertaId.isBlank()) {
            throw new DatoInvalidoException("ofertaId", "es obligatorio");
        }
        ofertaId = ofertaId.strip();
        if (fechaInicio == null) {
            throw new DatoInvalidoException("fechaInicio", "es obligatoria");
        }
        if (personas == null) {
            throw new DatoInvalidoException("personas", "es obligatorio");
        }
    }
}
