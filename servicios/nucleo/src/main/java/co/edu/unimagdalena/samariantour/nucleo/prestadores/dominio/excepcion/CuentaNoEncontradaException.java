package co.edu.unimagdalena.samariantour.nucleo.prestadores.dominio.excepcion;

import java.util.UUID;

/** La identidad no corresponde a una cuenta de prestador activa (módulo de cuentas). */
public class CuentaNoEncontradaException extends ExcepcionDominio {

    public CuentaNoEncontradaException(UUID id) {
        super("CUENTA_NO_ENCONTRADA", "No hay una cuenta de prestador activa %s".formatted(id));
    }
}
