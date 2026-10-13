package co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.puerto.salida;

import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.PerfilPublico;

import java.util.Optional;
import java.util.UUID;

/**
 * Almacenamiento de los perfiles públicos de los prestadores (RF-24). En el prototipo solo se leen.
 */
public interface PerfilRepositoryPort {

    /** Perfil público del prestador; vacío si aún no lo ha completado. */
    Optional<PerfilPublico> buscarPorPrestador(UUID prestadorId);
}
