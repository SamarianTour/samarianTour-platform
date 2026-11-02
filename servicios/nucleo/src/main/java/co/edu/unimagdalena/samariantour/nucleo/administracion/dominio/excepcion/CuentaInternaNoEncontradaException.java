package co.edu.unimagdalena.samariantour.nucleo.administracion.dominio.excepcion;

import java.util.UUID;

/** No hay una cuenta interna con ese identificador: no existe, o es de un turista o un prestador (RF-81). */
public class CuentaInternaNoEncontradaException extends ExcepcionDominio {

    public CuentaInternaNoEncontradaException(UUID usuarioId) {
        super("CUENTA_NO_ENCONTRADA", "No hay una cuenta interna %s".formatted(usuarioId));
    }
}
