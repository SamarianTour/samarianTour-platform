package co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo;

import java.util.List;
import java.util.function.Function;

/**
 * Una página de resultados y el total de elementos que cumplen el criterio.
 */
public record Pagina<T>(List<T> contenido, Paginacion paginacion, long totalElementos) {

    public Pagina {
        contenido = List.copyOf(contenido);
    }

    public long totalPaginas() {
        return (totalElementos + paginacion.tamano() - 1) / paginacion.tamano();
    }

    public <R> Pagina<R> map(Function<T, R> transformacion) {
        return new Pagina<>(contenido.stream().map(transformacion).toList(), paginacion, totalElementos);
    }
}
