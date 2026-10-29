package co.edu.unimagdalena.samariantour.nucleo.administracion.dominio.excepcion;

import java.util.UUID;

/**
 * El prestador de la oferta no tiene el RNT activo: no se le puede aprobar (RF-79, DC-38, DN-35). Rechazarla sí se
 * puede.
 */
public class PrestadorNoHabilitadoException extends ExcepcionDominio {

    public PrestadorNoHabilitadoException(UUID prestadorId) {
        super("PRESTADOR_NO_HABILITADO", "El prestador %s no tiene el RNT activo: su oferta no se puede aprobar"
                .formatted(prestadorId));
    }
}
