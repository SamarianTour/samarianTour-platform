package co.edu.unimagdalena.samariantour.catalogo.catalogo.infraestructura.entrada.rest.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

/**
 * Perfil público del prestador que se muestra en la ficha (RF-24). No incluye datos de contacto directo.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record PerfilPublicoResponse(
        String nombreComercial,
        String descripcion,
        List<String> fotos,
        String horariosAtencion,
        String sitioWeb,
        List<String> redesSociales) {
}
