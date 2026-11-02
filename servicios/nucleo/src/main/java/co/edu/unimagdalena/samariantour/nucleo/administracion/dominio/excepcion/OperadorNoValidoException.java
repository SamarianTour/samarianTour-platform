package co.edu.unimagdalena.samariantour.nucleo.administracion.dominio.excepcion;

import java.util.UUID;

/** La cuenta que se quiere asignar no existe, no es de un operador de disponibilidad o está desactivada (RF-32). */
public class OperadorNoValidoException extends ExcepcionDominio {

    public OperadorNoValidoException(UUID operadorId) {
        super("OPERADOR_NO_VALIDO", "La cuenta %s no es de un operador de disponibilidad activo".formatted(operadorId));
    }
}
