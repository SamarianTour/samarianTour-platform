package co.edu.unimagdalena.samariantour.catalogo.catalogo.infraestructura.entrada.rest.dto;

import java.util.List;

/**
 * Los datos de una zona que escribe el administrador (HU-ADM-06, DC-49). Al registrarla no se envía {@code activa}:
 * nace activa. Al editarla es obligatoria, para que inactivar o reactivar sea siempre explícito.
 *
 * @param areaProtegida si no se envía, false
 * @param altaAfluencia si no se envía, false
 */
public record ZonaRequest(
        String nombre,
        String descripcion,
        CoordenadaDto ubicacion,
        Boolean areaProtegida,
        Boolean altaAfluencia,
        List<String> buenasPracticas,
        String condicionesVisita,
        Boolean activa) {
}
