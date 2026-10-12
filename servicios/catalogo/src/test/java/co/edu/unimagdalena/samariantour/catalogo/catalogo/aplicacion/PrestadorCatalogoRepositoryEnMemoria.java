package co.edu.unimagdalena.samariantour.catalogo.catalogo.aplicacion;

import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.PrestadorCatalogo;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.puerto.salida.PrestadorCatalogoRepositoryPort;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/** Guarda copias, igual que una base de datos: un cambio que no se guarda no queda registrado. */
class PrestadorCatalogoRepositoryEnMemoria implements PrestadorCatalogoRepositoryPort {

    private final Map<UUID, PrestadorCatalogo> prestadores = new HashMap<>();

    @Override
    public Optional<PrestadorCatalogo> buscarPorId(UUID prestadorId) {
        return Optional.ofNullable(prestadores.get(prestadorId)).map(PrestadorCatalogoRepositoryEnMemoria::copia);
    }

    @Override
    public void guardar(PrestadorCatalogo prestador) {
        prestadores.put(prestador.prestadorId(), copia(prestador));
    }

    private static PrestadorCatalogo copia(PrestadorCatalogo prestador) {
        return new PrestadorCatalogo(prestador.prestadorId(), prestador.rntActivo(), prestador.version());
    }
}
