package co.edu.unimagdalena.samariantour.catalogo.opiniones.infraestructura.entrada.rest.dto;

import java.util.List;

/**
 * Una página de resultados, con la misma forma que en el catálogo. {@code pagina} empieza en 0 (DC-14).
 */
public record PaginaResponse<T>(List<T> contenido, int pagina, int tamano, long totalElementos, long totalPaginas) {
}
