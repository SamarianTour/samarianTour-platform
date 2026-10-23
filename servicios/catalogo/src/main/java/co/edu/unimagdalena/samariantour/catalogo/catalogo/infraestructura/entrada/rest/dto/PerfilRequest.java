package co.edu.unimagdalena.samariantour.catalogo.catalogo.infraestructura.entrada.rest.dto;

import java.util.List;

/**
 * Los datos del perfil público que escribe el prestador (RF-64). Las fotos se suben y se quitan aparte.
 *
 * @param descripcion obligatoria; los demás campos son opcionales (DC-26)
 */
public record PerfilRequest(
        String nombreComercial,
        String descripcion,
        String horariosAtencion,
        String sitioWeb,
        List<String> redesSociales) {
}
