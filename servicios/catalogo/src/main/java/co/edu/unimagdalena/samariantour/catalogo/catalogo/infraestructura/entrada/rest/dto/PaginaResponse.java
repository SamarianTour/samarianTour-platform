package co.edu.unimagdalena.samariantour.catalogo.catalogo.infraestructura.entrada.rest.dto;

import java.util.List;

/**
 * Una página de resultados. {@code pagina} empieza en 0 (DC-14).
 */
public record PaginaResponse<T>(List<T> contenido, int pagina, int tamano, long totalElementos, long totalPaginas) {
}
