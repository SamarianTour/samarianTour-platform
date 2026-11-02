package co.edu.unimagdalena.samariantour.nucleo.solicitudes.api;

/**
 * @param categoria GUIA_INDEPENDIENTE, MICRO_PEQUENO, MEDIANO o GRANDE; vacía si el prestador de las solicitudes
 *                  anteriores a la copia no se encontró
 * @param pagadas   las que están Pagadas
 */
public record CategoriaContada(String categoria, long solicitudes, long pagadas) {
}
