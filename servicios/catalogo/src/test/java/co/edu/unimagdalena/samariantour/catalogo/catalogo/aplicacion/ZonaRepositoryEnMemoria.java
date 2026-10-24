package co.edu.unimagdalena.samariantour.catalogo.catalogo.aplicacion;

import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.excepcion.ZonaDuplicadaException;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.Zona;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.puerto.salida.ZonaRepositoryPort;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Doble en memoria de las zonas, con el mismo nombre único que el adaptador real (DC-49). */
class ZonaRepositoryEnMemoria implements ZonaRepositoryPort {

    private final Map<String, Zona> zonas = new LinkedHashMap<>();
    private int siguienteId = 1;

    ZonaRepositoryEnMemoria(Zona... zonas) {
        for (Zona zona : zonas) {
            this.zonas.put(zona.id(), zona);
        }
    }

    @Override
    public Optional<Zona> buscarPorId(String id) {
        return Optional.ofNullable(zonas.get(id));
    }

    @Override
    public List<Zona> buscarActivas() {
        return buscarTodas().stream().filter(Zona::activa).toList();
    }

    @Override
    public List<Zona> buscarTodas() {
        return zonas.values().stream().sorted(Comparator.comparing(Zona::nombre)).toList();
    }

    @Override
    public String nuevoId() {
        return "zona-nueva-" + siguienteId++;
    }

    @Override
    public void guardar(Zona zona) {
        boolean repetido = zonas.values().stream()
                .anyMatch(otra -> !otra.id().equals(zona.id()) && otra.claveNombre().equals(zona.claveNombre()));
        if (repetido) {
            throw new ZonaDuplicadaException(zona.nombre());
        }
        zonas.put(zona.id(), zona);
    }
}
