package co.edu.unimagdalena.samariantour.catalogo.catalogo.infraestructura.entrada.rest.dto;

import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.NivelEsfuerzo;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.TipoActividad;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.TipoOferta;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Una oferta visible para la recomendación (RF-26, RF-29): los datos de sus criterios explícitos y el prestador,
 * para el registro de exposición (RET-02). No lleva calificaciones, comisión ni tamaño del prestador.
 *
 * @param nivelEsfuerzo solo actividad y paquete
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record OfertaVisibleResponse(
        String id,
        TipoOferta tipo,
        String nombre,
        String descripcion,
        UUID prestadorId,
        String zonaId,
        boolean zonaAltaAfluencia,
        BigDecimal precioReferencia,
        String unidadPrecio,
        List<TipoActividad> tiposActividad,
        NivelEsfuerzo nivelEsfuerzo,
        boolean accesibleSillaRuedas) {
}
