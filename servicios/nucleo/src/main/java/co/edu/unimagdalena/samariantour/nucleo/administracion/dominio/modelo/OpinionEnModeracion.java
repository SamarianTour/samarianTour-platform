package co.edu.unimagdalena.samariantour.nucleo.administracion.dominio.modelo;

import java.util.UUID;

/** Lo que el catálogo dice de una opinión en moderación (DC-44 del catálogo): a quién notificar. */
public record OpinionEnModeracion(String opinionId, String ofertaId, UUID turistaId) {
}
