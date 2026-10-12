package co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo;

import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.excepcion.DatoInvalidoException;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * Filtros de la búsqueda pública de ofertas (RF-14 a RF-17). Todos son opcionales; la búsqueda devuelve solo
 * ofertas visibles, ordenadas por precio de referencia y luego por id para que la paginación sea estable.
 */
public record CriteriosBusqueda(
        TipoOferta tipo,
        String zonaId,
        BigDecimal precioMin,
        BigDecimal precioMax,
        Paginacion paginacion) {

    public CriteriosBusqueda {
        zonaId = Validaciones.textoOpcional(zonaId);
        if (precioMin != null) {
            Validaciones.noNegativo(precioMin, "precioMin");
        }
        if (precioMax != null) {
            Validaciones.noNegativo(precioMax, "precioMax");
        }
        if (precioMin != null && precioMax != null && precioMin.compareTo(precioMax) > 0) {
            throw new DatoInvalidoException("precioMin", "no puede ser mayor que precioMax");
        }
        Objects.requireNonNull(paginacion, "paginacion");
    }
}
