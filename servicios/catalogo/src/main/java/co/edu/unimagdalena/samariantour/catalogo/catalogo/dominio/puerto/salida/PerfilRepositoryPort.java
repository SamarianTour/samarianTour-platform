package co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.puerto.salida;

import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.PerfilPublico;

import java.util.Optional;
import java.util.UUID;

/**
 * Almacenamiento de los perfiles públicos de los prestadores (RF-24, RF-64). Hay a lo sumo uno por prestador.
 */
public interface PerfilRepositoryPort {

    /** Perfil público del prestador; vacío si aún no lo ha completado. */
    Optional<PerfilPublico> buscarPorPrestador(UUID prestadorId);

    /**
     * Crea el perfil o reemplaza el guardado del mismo prestador, solo si su revisión sigue siendo la que se leyó
     * (bloqueo optimista, ADR-05). Si otra escritura lo cambió en medio, o si se crea uno para un prestador que ya
     * tiene, lanza ConflictoConcurrenciaException y no guarda nada.
     */
    void guardar(PerfilPublico perfil);
}
