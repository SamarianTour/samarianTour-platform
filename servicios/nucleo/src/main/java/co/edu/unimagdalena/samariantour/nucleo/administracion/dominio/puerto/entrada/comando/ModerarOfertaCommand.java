package co.edu.unimagdalena.samariantour.nucleo.administracion.dominio.puerto.entrada.comando;

import co.edu.unimagdalena.samariantour.nucleo.administracion.dominio.modelo.Decision;

import java.util.UUID;

/**
 * @param numeroVersion   la versión que el administrador revisó en la cola del catálogo
 * @param administradorId el administrador de la identidad verificada
 */
public record ModerarOfertaCommand(String ofertaId, int numeroVersion, Decision decision, String motivo,
                                   UUID administradorId) {
}
