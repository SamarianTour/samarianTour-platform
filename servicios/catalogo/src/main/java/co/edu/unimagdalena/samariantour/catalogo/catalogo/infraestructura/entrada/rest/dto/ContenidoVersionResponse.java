package co.edu.unimagdalena.samariantour.catalogo.catalogo.infraestructura.entrada.rest.dto;

import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.ModalidadRecorrido;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.NivelEsfuerzo;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.TipoActividad;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.math.BigDecimal;
import java.util.List;

/**
 * El contenido de una versión tal como lo envió el prestador, con los mismos campos que {@link OfertaRequest}, para
 * que el administrador lo revise (RF-79). Los campos que no corresponden al tipo de la oferta se omiten.
 *
 * @param accesibleSillaRuedas ACTIVIDAD, ALOJAMIENTO y RESTAURANTE; el de un paquete se calcula con sus actividades
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ContenidoVersionResponse(
        String nombre,
        String descripcion,
        BigDecimal precioReferencia,
        String unidadPrecio,
        String horarios,
        String zonaId,
        CoordenadaDto ubicacion,
        String comoLlegar,
        int capacidadMaxima,
        TipoActividad tipoActividad,
        NivelEsfuerzo nivelEsfuerzo,
        Boolean accesibleSillaRuedas,
        List<String> horariosSalida,
        String puntoEncuentro,
        Integer duracionMinutos,
        ModalidadRecorrido modalidad,
        List<String> idiomasAtencion,
        List<String> actividadIds,
        String tipoUnidad) {
}
