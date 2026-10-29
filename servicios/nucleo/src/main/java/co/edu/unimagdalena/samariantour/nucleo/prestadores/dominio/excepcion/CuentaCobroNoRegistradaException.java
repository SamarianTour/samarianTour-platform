package co.edu.unimagdalena.samariantour.nucleo.prestadores.dominio.excepcion;

import java.util.UUID;

/** El prestador todavía no registró su cuenta de cobro (RF-74). */
public class CuentaCobroNoRegistradaException extends ExcepcionDominio {

    public CuentaCobroNoRegistradaException(UUID prestadorId) {
        super("CUENTA_COBRO_NO_REGISTRADA", "El prestador %s todavía no registró su cuenta de cobro"
                .formatted(prestadorId));
    }
}
