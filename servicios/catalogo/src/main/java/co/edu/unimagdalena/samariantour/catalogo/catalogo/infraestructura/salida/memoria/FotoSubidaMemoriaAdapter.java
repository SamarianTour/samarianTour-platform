package co.edu.unimagdalena.samariantour.catalogo.catalogo.infraestructura.salida.memoria;

import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.Foto;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.FotoSubida;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.puerto.salida.FotoSubidaRepositoryPort;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.infraestructura.salida.demo.DatosSantaMarta;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Registro de las fotos subidas en la memoria del proceso (DC-47). Marcar el uso y borrar son atómicos por foto, como
 * en MongoDB.
 */
@Component
@Profile("memoria")
public class FotoSubidaMemoriaAdapter implements FotoSubidaRepositoryPort {

    private final ConcurrentMap<Foto, FotoSubida> fotos = new ConcurrentHashMap<>();

    /** Arranca con las fotos de las ofertas de demostración. */
    public FotoSubidaMemoriaAdapter() {
        this(DatosSantaMarta.fotos());
    }

    /** Arranca con esas fotos registradas. */
    public FotoSubidaMemoriaAdapter(Collection<FotoSubida> iniciales) {
        iniciales.forEach(this::registrar);
    }

    @Override
    public void registrar(FotoSubida foto) {
        fotos.put(foto.foto(), foto);
    }

    @Override
    public List<FotoSubida> buscar(Collection<Foto> buscadas) {
        return buscadas.stream().distinct().map(fotos::get).filter(Objects::nonNull).toList();
    }

    @Override
    public void marcarEnUso(Collection<Foto> usadas, Instant ahora) {
        usadas.forEach(foto -> fotos.computeIfPresent(foto, (id, subida) -> subida.ultimoUso().isAfter(ahora)
                ? subida
                : new FotoSubida(subida.foto(), subida.prestadorId(), subida.destino(), ahora)));
    }

    @Override
    public List<FotoSubida> sinUsoDesde(Instant limite, int maximo) {
        return fotos.values().stream()
                .filter(subida -> subida.ultimoUso().isBefore(limite))
                .sorted(Comparator.comparing(FotoSubida::ultimoUso).thenComparing(subida -> subida.foto().id()))
                .limit(maximo)
                .toList();
    }

    @Override
    public boolean eliminarSiSigueSinUso(FotoSubida leida) {
        AtomicBoolean eliminada = new AtomicBoolean();
        fotos.computeIfPresent(leida.foto(), (id, actual) -> {
            if (actual.ultimoUso().isAfter(leida.ultimoUso())) {
                return actual;
            }
            eliminada.set(true);
            return null;
        });
        return eliminada.get();
    }
}
