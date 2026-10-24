package co.edu.unimagdalena.samariantour.recomendacion.recomendacion.infraestructura.salida.catalogo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/**
 * Esquema {@code PaginaOfertasVisibles} del contrato del catálogo.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
record PaginaOfertasJson(List<OfertaVisibleJson> contenido, int pagina, long totalPaginas) {
}
