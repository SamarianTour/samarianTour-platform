package co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.modelo;

/**
 * Las solicitudes enviadas y las pagadas de una categoría de prestador (RF-89).
 *
 * @param categoria vacía en las solicitudes anteriores cuyo prestador no se encontró (V14)
 */
public record ConteoCategoria(CategoriaPrestador categoria, long solicitudes, long pagadas) {
}
