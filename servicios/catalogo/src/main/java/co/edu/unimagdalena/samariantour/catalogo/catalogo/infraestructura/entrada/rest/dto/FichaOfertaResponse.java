package co.edu.unimagdalena.samariantour.catalogo.catalogo.infraestructura.entrada.rest.dto;

import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.ModalidadRecorrido;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.NivelEsfuerzo;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.TipoActividad;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.TipoOferta;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

/**
 * Ficha pública de una oferta (RF-20). Según el tipo de la oferta trae uno de los detalles {@code actividad},
 * {@code paquete} o {@code alojamiento}; el restaurante no tiene datos propios.
 *
 * @param accesibleSillaRuedas el de un paquete es true solo si todas sus actividades lo son (RF-17)
 * @param zona                 ausente si la zona ya no existe
 * @param perfilPrestador      ausente si el prestador no ha completado su perfil público (RF-24)
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record FichaOfertaResponse(
        String id,
        TipoOferta tipo,
        String nombre,
        String descripcion,
        BigDecimal precioReferencia,
        String unidadPrecio,
        String horarios,
        CoordenadaDto ubicacion,
        String comoLlegar,
        int capacidadMaxima,
        boolean accesibleSillaRuedas,
        DetalleActividad actividad,
        DetallePaquete paquete,
        DetalleAlojamiento alojamiento,
        ZonaResponse zona,
        PerfilPublicoResponse perfilPrestador) {

    /** @param horariosSalida horas en formato HH:mm */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record DetalleActividad(
            TipoActividad tipoActividad,
            NivelEsfuerzo nivelEsfuerzo,
            List<String> horariosSalida,
            String puntoEncuentro,
            Integer duracionMinutos,
            ModalidadRecorrido modalidad,
            List<String> idiomasAtencion) {
    }

    /**
     * @param tiposActividad los de sus actividades (DC-24)
     * @param nivelEsfuerzo  el mayor de sus actividades (RF-17)
     * @param horariosSalida horas en formato HH:mm
     */
    public record DetallePaquete(
            List<ActividadIncluidaResponse> actividades,
            Set<TipoActividad> tiposActividad,
            NivelEsfuerzo nivelEsfuerzo,
            int duracionMinutos,
            List<String> horariosSalida) {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record DetalleAlojamiento(String tipoUnidad) {
    }
}
