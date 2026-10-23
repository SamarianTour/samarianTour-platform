package co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.excepcion;

import java.util.UUID;

/**
 * El prestador todavía no ha creado su perfil público (RF-64). Hay que crearlo, con su descripción, antes de agregarle
 * fotos.
 */
public class PerfilNoEncontradoException extends ExcepcionDominio {

    public PerfilNoEncontradoException(UUID prestadorId) {
        super("PERFIL_NO_ENCONTRADO", "El prestador %s todavía no tiene perfil público".formatted(prestadorId));
    }
}
