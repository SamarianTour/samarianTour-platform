package co.edu.unimagdalena.samariantour.catalogo.opiniones.dominio.modelo;

import java.util.List;
import java.util.function.Function;

/**
 * Una página de resultados y el total de elementos que cumplen el criterio. Es la misma forma que la del catálogo,
 * pero propia del módulo: un módulo solo usa el paquete {@code api} de otro (DC-39).
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
