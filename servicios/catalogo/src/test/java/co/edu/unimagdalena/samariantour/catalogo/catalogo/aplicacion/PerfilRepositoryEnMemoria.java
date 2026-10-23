package co.edu.unimagdalena.samariantour.catalogo.catalogo.aplicacion;

import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.Foto;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.PerfilPublico;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.puerto.salida.PerfilRepositoryPort;
import co.edu.unimagdalena.samariantour.catalogo.compartido.concurrencia.ConflictoConcurrenciaException;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/** Doble en memoria con el mismo bloqueo optimista que los adaptadores (ADR-05). */
class PerfilRepositoryEnMemoria implements PerfilRepositoryPort {

    private final Map<UUID, PerfilPublico> perfiles = new HashMap<>();

    /** Cuántas veces más el próximo guardado choca con otra escritura, para probar los reintentos. */
    private int conflictosPendientes;

    @Override
    public Optional<PerfilPublico> buscarPorPrestador(UUID prestadorId) {
        return Optional.ofNullable(perfiles.get(prestadorId));
    }

    @Override
    public void guardar(PerfilPublico perfil) {
        PerfilPublico guardado = perfiles.get(perfil.prestadorId());
        long revisionGuardada = guardado == null ? PerfilPublico.REVISION_NUEVA : guardado.revision();
        if (conflictosPendientes > 0 || revisionGuardada != perfil.revision()) {
            conflictosPendientes = Math.max(0, conflictosPendientes - 1);
            throw new ConflictoConcurrenciaException("El perfil cambió desde que se leyó");
        }
        perfiles.put(perfil.prestadorId(), new PerfilPublico(perfil.prestadorId(), perfil.nombreComercial(),
                perfil.descripcion(), perfil.fotos(), perfil.horariosAtencion(), perfil.sitioWeb(),
                perfil.redesSociales(), revisionGuardada + 1));
    }

    @Override
    public Set<Foto> fotosEnUso(Collection<Foto> fotos) {
        return perfiles.values().stream()
                .flatMap(perfil -> perfil.fotos().stream())
                .filter(fotos::contains)
                .collect(Collectors.toSet());
    }

    void simularConflictos(int veces) {
        conflictosPendientes = veces;
    }
}
