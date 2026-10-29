package co.edu.unimagdalena.samariantour.nucleo.prestadores.dominio.excepcion;

import java.util.UUID;

/** El prestador todavía no registró su RNT (RF-62). */
public class PrestadorNoRegistradoException extends ExcepcionDominio {

    public PrestadorNoRegistradoException(UUID prestadorId) {
        super("PRESTADOR_NO_REGISTRADO", "El prestador %s todavía no registró su RNT".formatted(prestadorId));
    }
}
