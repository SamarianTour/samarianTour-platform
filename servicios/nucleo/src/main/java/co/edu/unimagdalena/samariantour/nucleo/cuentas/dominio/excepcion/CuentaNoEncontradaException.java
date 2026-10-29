package co.edu.unimagdalena.samariantour.nucleo.cuentas.dominio.excepcion;

import java.util.UUID;

/** No hay una cuenta con ese identificador, o no es del rol que la operación pide. */
public class CuentaNoEncontradaException extends ExcepcionDominio {

    public CuentaNoEncontradaException(UUID id) {
        super("CUENTA_NO_ENCONTRADA", "No hay una cuenta %s que pueda hacer esta operación".formatted(id));
    }
}
