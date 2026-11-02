package co.edu.unimagdalena.samariantour.nucleo.administracion.infraestructura.entrada.rest.dto;

import java.util.UUID;

/** @param operadorId la cuenta del operador de disponibilidad que atenderá al prestador (RF-32) */
public record AsignacionOperadorRequest(UUID operadorId) {
}
