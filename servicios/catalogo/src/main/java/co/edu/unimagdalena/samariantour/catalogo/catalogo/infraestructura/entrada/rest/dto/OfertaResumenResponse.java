package co.edu.unimagdalena.samariantour.catalogo.catalogo.infraestructura.entrada.rest.dto;

import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.TipoOferta;

import java.math.BigDecimal;

/**
 * Una oferta en los resultados de la búsqueda. El detalle completo está en la ficha.
 *
 * @param portada la primera foto de la versión publicada, para la tarjeta; null si es anterior a DC-47 y no tiene
 */
public record OfertaResumenResponse(
        String id,
        TipoOferta tipo,
        String nombre,
        String descripcion,
        BigDecimal precioReferencia,
        String unidadPrecio,
        String zonaId,
        FotoResponse portada) {
}
