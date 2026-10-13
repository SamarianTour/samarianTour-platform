package co.edu.unimagdalena.samariantour.catalogo.catalogo.infraestructura.salida.memoria;

import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.PrestadorCatalogo;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.puerto.salida.PrestadorCatalogoRepositoryPort;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.infraestructura.salida.demo.DatosSantaMarta;
import co.edu.unimagdalena.samariantour.catalogo.compartido.concurrencia.ConflictoConcurrenciaException;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Réplica de la habilitación RNT de los prestadores en la memoria del proceso. Guarda y entrega copias.
 */
@Component
@Profile("memoria")
public class PrestadorMemoriaAdapter implements PrestadorCatalogoRepositoryPort {

    private final Map<UUID, PrestadorCatalogo> prestadores = new ConcurrentHashMap<>();

    /** Arranca con los prestadores de demostración. */
    public PrestadorMemoriaAdapter() {
        this(DatosSantaMarta.prestadores());
    }

    public PrestadorMemoriaAdapter(Collection<PrestadorCatalogo> iniciales) {
        iniciales.forEach(prestador -> prestadores.put(prestador.prestadorId(), copia(prestador)));
    }

    @Override
    public Optional<PrestadorCatalogo> buscarPorId(UUID prestadorId) {
        return Optional.ofNullable(prestadores.get(prestadorId)).map(PrestadorMemoriaAdapter::copia);
    }

    @Override
    public void guardar(PrestadorCatalogo prestador) {
        // compute es atómico por clave: simula el guardado condicionado de MongoDB (ADR-05).
        prestadores.compute(prestador.prestadorId(), (id, guardado) -> {
            if (guardado != null && guardado.version() >= prestador.version()) {
                throw new ConflictoConcurrenciaException("La habilitación del prestador %s ya tiene la versión %d"
                        .formatted(id, guardado.version()));
            }
            return copia(prestador);
        });
    }

    private static PrestadorCatalogo copia(PrestadorCatalogo prestador) {
        return new PrestadorCatalogo(prestador.prestadorId(), prestador.rntActivo(), prestador.version());
    }
}
