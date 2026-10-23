package co.edu.unimagdalena.samariantour.catalogo.catalogo.infraestructura.salida.memoria;

import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.Foto;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.PerfilPublico;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.puerto.salida.PerfilRepositoryPort;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.infraestructura.salida.demo.DatosSantaMarta;
import co.edu.unimagdalena.samariantour.catalogo.compartido.concurrencia.ConflictoConcurrenciaException;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

/**
 * Perfiles públicos de los prestadores en la memoria del proceso, con el mismo bloqueo optimista que el adaptador de
 * MongoDB (ADR-05).
 */
@Component
@Profile("memoria")
public class PerfilMemoriaAdapter implements PerfilRepositoryPort {

    private final ConcurrentMap<UUID, PerfilPublico> perfiles = new ConcurrentHashMap<>();

    /** Arranca con los perfiles de demostración. */
    public PerfilMemoriaAdapter() {
        this(DatosSantaMarta.perfiles());
    }

    /** Arranca con esos perfiles, como si se hubieran guardado. */
    public PerfilMemoriaAdapter(Collection<PerfilPublico> perfiles) {
        perfiles.forEach(this::guardar);
    }

    @Override
    public Optional<PerfilPublico> buscarPorPrestador(UUID prestadorId) {
        return Optional.ofNullable(perfiles.get(prestadorId));
    }

    @Override
    public void guardar(PerfilPublico perfil) {
        perfiles.compute(perfil.prestadorId(), (prestadorId, guardado) -> {
            long revisionGuardada = guardado == null ? PerfilPublico.REVISION_NUEVA : guardado.revision();
            if (revisionGuardada != perfil.revision()) {
                throw new ConflictoConcurrenciaException(
                        "El perfil del prestador %s cambió desde que se leyó".formatted(prestadorId));
            }
            return conRevision(perfil, revisionGuardada + 1);
        });
    }

    @Override
    public Set<Foto> fotosEnUso(Collection<Foto> fotos) {
        return perfiles.values().stream()
                .flatMap(perfil -> perfil.fotos().stream())
                .filter(fotos::contains)
                .collect(Collectors.toSet());
    }

    private static PerfilPublico conRevision(PerfilPublico perfil, long revision) {
        return new PerfilPublico(perfil.prestadorId(), perfil.nombreComercial(), perfil.descripcion(), perfil.fotos(),
                perfil.horariosAtencion(), perfil.sitioWeb(), perfil.redesSociales(), revision);
    }
}
