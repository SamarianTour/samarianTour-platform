package co.edu.unimagdalena.samariantour.catalogo.catalogo.aplicacion;

import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.Foto;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.FotoSubida;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.puerto.salida.FotoSubidaRepositoryPort;

import java.time.Instant;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/** Doble en memoria del registro de fotos, con el mismo borrado condicionado que los adaptadores (DC-47). */
class FotoSubidaRepositoryEnMemoria implements FotoSubidaRepositoryPort {

    private final Map<Foto, FotoSubida> fotos = new HashMap<>();

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
        FotoSubida actual = fotos.get(leida.foto());
        if (actual == null || actual.ultimoUso().isAfter(leida.ultimoUso())) {
            return false;
        }
        fotos.remove(leida.foto());
        return true;
    }

    Optional<FotoSubida> de(Foto foto) {
        return Optional.ofNullable(fotos.get(foto));
    }

    int cantidad() {
        return fotos.size();
    }
}
