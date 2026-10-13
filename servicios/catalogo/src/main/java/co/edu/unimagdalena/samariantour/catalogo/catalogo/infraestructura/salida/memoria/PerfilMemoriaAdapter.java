package co.edu.unimagdalena.samariantour.catalogo.catalogo.infraestructura.salida.memoria;

import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.PerfilPublico;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.puerto.salida.PerfilRepositoryPort;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.infraestructura.salida.demo.DatosSantaMarta;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Perfiles públicos de los prestadores en la memoria del proceso. En el prototipo solo se leen.
 */
@Component
@Profile("memoria")
public class PerfilMemoriaAdapter implements PerfilRepositoryPort {

    private final Map<UUID, PerfilPublico> perfiles;

    /** Arranca con los perfiles de demostración. */
    public PerfilMemoriaAdapter() {
        this(DatosSantaMarta.perfiles());
    }

    public PerfilMemoriaAdapter(Collection<PerfilPublico> perfiles) {
        this.perfiles = perfiles.stream()
                .collect(Collectors.toUnmodifiableMap(PerfilPublico::prestadorId, Function.identity()));
    }

    @Override
    public Optional<PerfilPublico> buscarPorPrestador(UUID prestadorId) {
        return Optional.ofNullable(perfiles.get(prestadorId));
    }
}
