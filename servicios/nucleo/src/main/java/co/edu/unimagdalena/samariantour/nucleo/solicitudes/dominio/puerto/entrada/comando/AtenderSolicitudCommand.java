package co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.puerto.entrada.comando;

import co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.excepcion.DatoInvalidoException;
import co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.modelo.ResultadoAtencion;

import java.util.Objects;
import java.util.UUID;

/**
 * Lo que registra el operador (RF-37 a RF-39). Para marcarla Atendida hacen falta el precio base, la política de
 * cancelación y el aparte del cupo acordado con el prestador; para No disponible, nada más que una nota opcional.
 *
 * @param operadorId el de la identidad, nunca un campo del cuerpo
 */
public record AtenderSolicitudCommand(UUID operadorId, UUID solicitudId, ResultadoAtencion resultado, Long precioBase,
                                      String politicaCancelacion, Boolean aparteAcordado, String nota) {

    public AtenderSolicitudCommand {
        Objects.requireNonNull(operadorId, "operadorId");
        Objects.requireNonNull(solicitudId, "solicitudId");
        if (resultado == null) {
            throw new DatoInvalidoException("resultado", "debe ser ATENDIDA o NO_DISPONIBLE");
        }
        if (resultado == ResultadoAtencion.ATENDIDA) {
            if (precioBase == null) {
                throw new DatoInvalidoException("precioBase", "es obligatorio para marcarla ATENDIDA (RF-37)");
            }
            if (!Boolean.TRUE.equals(aparteAcordado)) {
                throw new DatoInvalidoException("aparteAcordado", ("debe ser true: el prestador tiene que haber "
                        + "aceptado apartar el cupo durante la ventana antes de marcarla ATENDIDA (RF-38, RF-39)"));
            }
        }
    }
}
