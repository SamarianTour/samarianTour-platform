package co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.puerto.entrada.comando;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Los datos del perfil público que escribe el prestador (RF-64). Las fotos se agregan y se quitan aparte.
 *
 * @param prestadorId el prestador de la identidad verificada; nunca uno que venga en el cuerpo de la petición
 */
public record GuardarPerfilCommand(
        UUID prestadorId,
        String nombreComercial,
        String descripcion,
        String horariosAtencion,
        String sitioWeb,
        List<String> redesSociales) {

    public GuardarPerfilCommand {
        Objects.requireNonNull(prestadorId, "prestadorId");
    }
}
