package co.edu.unimagdalena.samariantour.nucleo.administracion.dominio.excepcion;

import java.util.UUID;

/** El prestador todavía no registró su RNT: no está en la lista ni se le asigna operador (RF-62, RF-81). */
public class PrestadorNoRegistradoException extends ExcepcionDominio {

    public PrestadorNoRegistradoException(UUID prestadorId) {
        super("PRESTADOR_NO_REGISTRADO", "El prestador %s todavía no registró su RNT".formatted(prestadorId));
    }
}
