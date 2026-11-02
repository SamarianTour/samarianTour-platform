package co.edu.unimagdalena.samariantour.nucleo.indicadores.dominio.modelo;

/** @param categoria vacía en las solicitudes anteriores cuyo prestador no se encontró */
public record ConteoCategoria(String categoria, long solicitudes, long pagadas) {
}
