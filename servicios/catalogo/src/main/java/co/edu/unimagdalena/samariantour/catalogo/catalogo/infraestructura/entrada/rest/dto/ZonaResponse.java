package co.edu.unimagdalena.samariantour.catalogo.catalogo.infraestructura.entrada.rest.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ZonaResponse(
        String id,
        String nombre,
        String descripcion,
        CoordenadaDto ubicacion,
        boolean areaProtegida,
        boolean altaAfluencia,
        List<String> buenasPracticas,
        String condicionesVisita) {
}
