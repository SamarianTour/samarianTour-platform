package co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.politica;

import co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.excepcion.SolicitudDuplicadaException;
import co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.modelo.CamposDelTipo;
import co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.modelo.SolicitudDisponibilidad;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Collection;
import java.util.Objects;
import java.util.UUID;

/**
 * Un turista no puede tener dos solicitudes no terminales para la misma oferta, fecha de inicio y hora (RF-31). La hora
 * es la que aplica al tipo de oferta: el horario de salida o la hora de la reserva; el alojamiento se compara solo por
 * la fecha de inicio. Con dos envíos a la vez, la regla no alcanza a ver al otro: lo detiene el índice único parcial de
 * PostgreSQL (DN-11).
 */
public final class ReglaNoDuplicado {

    private ReglaNoDuplicado() {
    }

    /**
     * @param hora       la de {@link CamposDelTipo#hora()}: vacía en alojamiento
     * @param delTurista las solicitudes del turista que podrían chocar; la regla descarta las cerradas y las demás
     * @throws SolicitudDuplicadaException con la que ya existe
     */
    public static void verificar(UUID turistaId, String ofertaId, LocalDate fechaInicio, LocalTime hora,
                                 Collection<SolicitudDisponibilidad> delTurista) {
        delTurista.stream()
                .filter(existente -> !existente.estado().esTerminal())
                .filter(existente -> existente.esDelTurista(turistaId))
                .filter(existente -> existente.ofertaId().equals(ofertaId))
                .filter(existente -> existente.fechaInicio().equals(fechaInicio))
                .filter(existente -> Objects.equals(existente.campos().hora(), hora))
                .findFirst()
                .ifPresent(existente -> {
                    throw new SolicitudDuplicadaException(existente.id());
                });
    }
}
