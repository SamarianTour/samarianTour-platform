package co.edu.unimagdalena.samariantour.nucleo.cuentas.dominio.excepcion;

import java.time.Instant;

/** Cinco fallos consecutivos bloquearon la cuenta 15 minutos (RF-05); mientras tanto no se revisa la contraseña. */
public class CuentaBloqueadaException extends ExcepcionDominio {

    private final Instant bloqueadaHasta;

    public CuentaBloqueadaException(Instant bloqueadaHasta) {
        super("CUENTA_BLOQUEADA", "La cuenta está bloqueada por intentos fallidos hasta " + bloqueadaHasta);
        this.bloqueadaHasta = bloqueadaHasta;
    }

    public Instant bloqueadaHasta() {
        return bloqueadaHasta;
    }
}
