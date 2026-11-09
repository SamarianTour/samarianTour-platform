package co.edu.unimagdalena.samariantour.nucleo.cuentas.api;

import java.time.Instant;

/**
 * La contraseña no confirma la operación (RF-05, RF-13).
 *
 * @see #codigo() {@code CONTRASENA_INCORRECTA}, {@code CUENTA_BLOQUEADA} (con {@link #bloqueadaHasta()}) o
 *      {@code CUENTA_NO_ENCONTRADA}
 */
public class ContrasenaRechazadaException extends RuntimeException {

    private final String codigo;
    private final Instant bloqueadaHasta;

    public ContrasenaRechazadaException(String codigo, String mensaje, Instant bloqueadaHasta) {
        super(mensaje);
        this.codigo = codigo;
        this.bloqueadaHasta = bloqueadaHasta;
    }

    public String codigo() {
        return codigo;
    }

    /** Solo con {@code CUENTA_BLOQUEADA}. */
    public Instant bloqueadaHasta() {
        return bloqueadaHasta;
    }
}
