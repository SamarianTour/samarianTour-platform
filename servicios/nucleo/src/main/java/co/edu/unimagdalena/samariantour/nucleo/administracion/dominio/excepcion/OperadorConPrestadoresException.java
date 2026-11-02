package co.edu.unimagdalena.samariantour.nucleo.administracion.dominio.excepcion;

import java.util.UUID;

/** El operador todavía tiene prestadores asignados: primero hay que reasignárselos (DN-40). */
public class OperadorConPrestadoresException extends ExcepcionDominio {

    public OperadorConPrestadoresException(UUID operadorId, long asignados) {
        super("OPERADOR_CON_PRESTADORES", "El operador %s tiene %d %s: reasígnalos primero".formatted(operadorId,
                asignados, asignados == 1 ? "prestador asignado" : "prestadores asignados"));
    }
}
