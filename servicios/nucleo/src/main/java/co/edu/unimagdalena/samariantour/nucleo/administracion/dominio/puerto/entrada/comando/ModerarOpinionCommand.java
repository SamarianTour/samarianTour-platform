package co.edu.unimagdalena.samariantour.nucleo.administracion.dominio.puerto.entrada.comando;

import co.edu.unimagdalena.samariantour.nucleo.administracion.dominio.modelo.Decision;

import java.util.UUID;

/** @param administradorId el administrador de la identidad verificada */
public record ModerarOpinionCommand(String opinionId, Decision decision, String motivo, UUID administradorId) {
}
