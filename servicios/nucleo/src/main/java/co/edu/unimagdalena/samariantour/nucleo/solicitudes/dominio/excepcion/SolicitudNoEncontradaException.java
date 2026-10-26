package co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.excepcion;

import java.util.UUID;

/**
 * La solicitud no existe, o no es de quien la pide: la de otro turista o la asignada a otro operador responde lo mismo,
 * para no revelar que existe (DN-09).
 */
public class SolicitudNoEncontradaException extends ExcepcionDominio {

    public SolicitudNoEncontradaException(UUID solicitudId) {
        super("SOLICITUD_NO_ENCONTRADA", "La solicitud %s no existe".formatted(solicitudId));
    }
}
