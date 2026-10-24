package co.edu.unimagdalena.samariantour.catalogo.catalogo.infraestructura.entrada.rest.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

/**
 * Una zona, en la lista pública, en su ficha, en la de sus ofertas y en la del administrador.
 *
 * @param activa una zona inactiva no admite ofertas nuevas, pero sus ofertas siguen visibles (DC-49)
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ZonaResponse(
        String id,
        String nombre,
        String descripcion,
        CoordenadaDto ubicacion,
        boolean areaProtegida,
        boolean altaAfluencia,
        boolean activa,
        List<String> buenasPracticas,
        String condicionesVisita) {
}
