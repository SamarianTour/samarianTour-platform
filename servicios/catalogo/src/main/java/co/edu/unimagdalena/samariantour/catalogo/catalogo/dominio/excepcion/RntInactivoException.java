package co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.excepcion;

import java.util.UUID;

/**
 * El prestador no puede publicar ni editar ofertas sin el RNT activo (RF-79).
 */
public class RntInactivoException extends ExcepcionDominio {

    public RntInactivoException(UUID prestadorId) {
        super("RNT_INACTIVO", "El RNT del prestador %s no está activo".formatted(prestadorId));
    }
}
