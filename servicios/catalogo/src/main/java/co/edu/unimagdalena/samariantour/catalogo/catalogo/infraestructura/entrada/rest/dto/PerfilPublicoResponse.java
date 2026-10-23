package co.edu.unimagdalena.samariantour.catalogo.catalogo.infraestructura.entrada.rest.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

/**
 * Perfil público del prestador: el que se muestra en la ficha (RF-24) y el que el prestador gestiona (RF-64). No
 * incluye datos de contacto directo.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record PerfilPublicoResponse(
        String nombreComercial,
        String descripcion,
        List<FotoResponse> fotos,
        String horariosAtencion,
        String sitioWeb,
        List<String> redesSociales) {
}
