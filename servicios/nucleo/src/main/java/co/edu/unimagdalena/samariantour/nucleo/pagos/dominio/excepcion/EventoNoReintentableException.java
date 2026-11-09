package co.edu.unimagdalena.samariantour.nucleo.pagos.dominio.excepcion;

import java.util.UUID;

/** El evento no existe ({@code EVENTO_NO_ENCONTRADO}) o no está fallido ({@code EVENTO_NO_FALLIDO}) (DN-44). */
public class EventoNoReintentableException extends ExcepcionDominio {

    public static final String NO_ENCONTRADO = "EVENTO_NO_ENCONTRADO";
    public static final String NO_FALLIDO = "EVENTO_NO_FALLIDO";

    public EventoNoReintentableException(String codigo, UUID eventoId) {
        super(codigo, NO_ENCONTRADO.equals(codigo) ? "No existe el evento " + eventoId
                : "El evento " + eventoId + " no está fallido");
    }
}
