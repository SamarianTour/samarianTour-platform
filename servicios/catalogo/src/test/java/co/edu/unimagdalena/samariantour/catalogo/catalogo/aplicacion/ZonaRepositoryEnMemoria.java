package co.edu.unimagdalena.samariantour.catalogo.catalogo.aplicacion;

import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.Zona;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.puerto.salida.ZonaRepositoryPort;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

class ZonaRepositoryEnMemoria implements ZonaRepositoryPort {

    private final List<Zona> zonas;

    ZonaRepositoryEnMemoria(Zona... zonas) {
        this.zonas = List.of(zonas);
    }

    @Override
    public Optional<Zona> buscarPorId(String id) {
        return zonas.stream().filter(zona -> zona.id().equals(id)).findFirst();
    }

    @Override
    public List<Zona> buscarActivas() {
        return zonas.stream().filter(Zona::activa).sorted(Comparator.comparing(Zona::nombre)).toList();
    }
}
