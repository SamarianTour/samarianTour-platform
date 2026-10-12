package co.edu.unimagdalena.samariantour.catalogo.catalogo.aplicacion;

import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.PerfilPublico;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.puerto.salida.PerfilRepositoryPort;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

class PerfilRepositoryEnMemoria implements PerfilRepositoryPort {

    private final Map<UUID, PerfilPublico> perfiles = new HashMap<>();

    @Override
    public Optional<PerfilPublico> buscarPorPrestador(UUID prestadorId) {
        return Optional.ofNullable(perfiles.get(prestadorId));
    }

    void guardar(PerfilPublico perfil) {
        perfiles.put(perfil.prestadorId(), perfil);
    }
}
