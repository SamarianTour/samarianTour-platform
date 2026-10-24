package co.edu.unimagdalena.samariantour.catalogo.catalogo.infraestructura.entrada.rest.dto;

import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.EstadoModeracion;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Estado de moderación de una versión de la oferta, como lo ve su prestador.
 *
 * @param numero       número de la versión; es el que nombra el aviso de moderación (DC-05)
 * @param unidadPrecio a qué corresponde el precio según el tipo de oferta, como en la búsqueda (DC-22, DC-50)
 * @param portada      la primera foto de la versión; se omite si es anterior a DC-47 y no tiene
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record VersionResponse(
        int numero,
        EstadoModeracion estadoModeracion,
        String motivoRechazo,
        Instant enviadaEn,
        Instant revisadaEn,
        String nombre,
        BigDecimal precioReferencia,
        String unidadPrecio,
        FotoResponse portada) {
}
