package co.edu.unimagdalena.samariantour.nucleo.indicadores.dominio.modelo;

import java.util.UUID;

/** Cuántas veces salió un prestador en las recomendaciones de un periodo: dos ofertas en una consulta son dos. */
public record AparicionesPrestador(UUID prestadorId, long apariciones) {
}
