package co.edu.unimagdalena.samariantour.catalogo.catalogo.infraestructura.entrada.rest.dto;

import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.TipoOferta;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;
import java.util.UUID;

/**
 * Lo que el núcleo necesita de una oferta al crear una solicitud (RF-30): los datos que la solicitud copia (nombre,
 * tipo, zona y prestador) y los que valida (horarios de salida y capacidad máxima).
 *
 * @param horariosSalida horas "HH:mm" entre las que el turista elige; solo actividad y paquete
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record OfertaInternaResponse(
        String id,
        TipoOferta tipo,
        String nombre,
        UUID prestadorId,
        String zonaId,
        List<String> horariosSalida,
        int capacidadMaxima) {
}
