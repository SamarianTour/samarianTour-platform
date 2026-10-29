package co.edu.unimagdalena.samariantour.nucleo.pagos.dominio.puerto.salida;

import co.edu.unimagdalena.samariantour.nucleo.pagos.dominio.excepcion.PagoNoAplicableException;
import co.edu.unimagdalena.samariantour.nucleo.pagos.dominio.modelo.EfectoEnSolicitud;
import co.edu.unimagdalena.samariantour.nucleo.pagos.dominio.modelo.SolicitudCobrable;

import java.util.Optional;
import java.util.UUID;

/**
 * Las solicitudes, por {@code solicitudes.api} y nunca por su dominio ni sus tablas (DN-17). Los métodos que cambian la
 * solicitud corren en la transacción de pagos: el pago y la solicitud se confirman juntos. Si otra escritura la cambió
 * a la vez, lanzan {@code ConflictoConcurrenciaException} y pagos reintenta su transacción completa (ADR-05).
 *
 * @throws PagoNoAplicableException si la solicitud no existe o su estado no admite el evento
 */
public interface SolicitudesPort {

    /** La solicitud de ese turista, en cualquier estado; vacía si no existe o es de otro. */
    Optional<SolicitudCobrable> consultar(UUID solicitudId, UUID turistaId);

    /** Un intento empezó: el vencimiento espera su resultado hasta 30 min (RF-55). */
    EfectoEnSolicitud marcarIntentoEnCurso(UUID solicitudId);

    /** El intento quedó abandonado: deja de aplazar el vencimiento (DN-20). */
    EfectoEnSolicitud liberarIntentoEnCurso(UUID solicitudId);

    EfectoEnSolicitud registrarPagoAprobado(UUID solicitudId);

    EfectoEnSolicitud registrarPagoRechazado(UUID solicitudId);
}
