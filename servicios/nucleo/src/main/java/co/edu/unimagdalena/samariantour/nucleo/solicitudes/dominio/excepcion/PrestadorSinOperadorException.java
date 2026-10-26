package co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.excepcion;

import java.util.UUID;

/**
 * El prestador de la oferta no tiene un operador asignado, así que la solicitud no tendría quién la atienda (RF-32)
 * [SUPUESTO] (DN-15). No se crea; es un problema de configuración que corrige el administrador, así que además se
 * registra la incidencia (RF-32).
 */
public class PrestadorSinOperadorException extends ExcepcionDominio {

    private final UUID prestadorId;
    private final String ofertaId;

    public PrestadorSinOperadorException(UUID prestadorId, String ofertaId) {
        super("PRESTADOR_SIN_OPERADOR", ("El prestador %s no tiene un operador de disponibilidad asignado: por "
                + "ahora no se pueden enviar solicitudes a sus ofertas").formatted(prestadorId));
        this.prestadorId = prestadorId;
        this.ofertaId = ofertaId;
    }

    public UUID prestadorId() {
        return prestadorId;
    }

    public String ofertaId() {
        return ofertaId;
    }
}
