package co.edu.unimagdalena.samariantour.nucleo.prestadores.dominio.excepcion;

import java.time.Instant;

/**
 * La contraseña no confirma la supresión (RF-05, RF-13): {@code CONTRASENA_INCORRECTA}, {@code CUENTA_BLOQUEADA} (con
 * {@link #bloqueadaHasta()}) o {@code CUENTA_NO_ENCONTRADA}.
 */
public class ContrasenaRechazadaException extends ExcepcionDominio {

    private final Instant bloqueadaHasta;

    public ContrasenaRechazadaException(String codigo, String mensaje, Instant bloqueadaHasta) {
        super(codigo, mensaje);
        this.bloqueadaHasta = bloqueadaHasta;
    }

    public Instant bloqueadaHasta() {
        return bloqueadaHasta;
    }
}
