package co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.modelo;

import java.util.List;
import java.util.function.Function;

/** Una página de resultados con el total, como las del catálogo. */
public record Pagina<T>(List<T> contenido, int pagina, int tamano, long totalElementos) {

    public Pagina {
        contenido = List.copyOf(contenido);
    }

    public int totalPaginas() {
        return (int) ((totalElementos + tamano - 1) / tamano);
    }

    public <R> Pagina<R> map(Function<? super T, ? extends R> conversion) {
        return new Pagina<>(contenido.stream().<R>map(conversion).toList(), pagina, tamano, totalElementos);
    }
}
