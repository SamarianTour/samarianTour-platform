package co.edu.unimagdalena.samariantour.catalogo.catalogo.infraestructura.entrada.rest.dto;

import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.EstadoModeracion;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Estado de moderación de una versión de la oferta, como lo ve su prestador.
 *
 * @param numero número de la versión; es el que nombra el aviso de moderación (DC-05)
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record VersionResponse(
        int numero,
        EstadoModeracion estadoModeracion,
        String motivoRechazo,
        Instant enviadaEn,
        Instant revisadaEn,
        String nombre,
        BigDecimal precioReferencia) {
}
