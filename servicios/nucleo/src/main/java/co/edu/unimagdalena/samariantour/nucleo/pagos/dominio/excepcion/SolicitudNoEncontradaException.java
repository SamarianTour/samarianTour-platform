package co.edu.unimagdalena.samariantour.nucleo.pagos.dominio.excepcion;

import java.util.UUID;

/** La solicitud no existe o es de otro turista: responde lo mismo en los dos casos, para no revelar que existe. */
public class SolicitudNoEncontradaException extends ExcepcionDominio {

    public SolicitudNoEncontradaException(UUID solicitudId) {
        super("SOLICITUD_NO_ENCONTRADA", "La solicitud " + solicitudId + " no existe");
    }
}
