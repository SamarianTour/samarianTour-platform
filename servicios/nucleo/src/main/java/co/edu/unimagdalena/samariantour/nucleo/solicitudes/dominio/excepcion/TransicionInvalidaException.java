package co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.excepcion;

import co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.modelo.EstadoSolicitud;

/**
 * La solicitud no admite ese cambio en su estado actual (DN-09): por ejemplo, confirmar una que ya se declinó, o
 * atender una que venció mientras tanto. El cliente debe volver a consultar el estado (RF-33).
 */
public class TransicionInvalidaException extends ExcepcionDominio {

    public TransicionInvalidaException(EstadoSolicitud actual, EstadoSolicitud pedido) {
        super("TRANSICION_INVALIDA", "La solicitud está %s y no puede pasar a %s".formatted(actual, pedido));
    }

    public TransicionInvalidaException(EstadoSolicitud actual, String operacion) {
        super("TRANSICION_INVALIDA", "La solicitud está %s y no admite %s".formatted(actual, operacion));
    }
}
