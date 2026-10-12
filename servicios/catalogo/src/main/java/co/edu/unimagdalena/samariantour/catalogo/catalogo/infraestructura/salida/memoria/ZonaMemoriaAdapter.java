package co.edu.unimagdalena.samariantour.catalogo.catalogo.infraestructura.salida.memoria;

import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.Zona;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.puerto.salida.ZonaRepositoryPort;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Zonas en la memoria del proceso. En el prototipo solo se leen.
 */
@Component
@Profile("memoria")
public class ZonaMemoriaAdapter implements ZonaRepositoryPort {

    private final List<Zona> zonas;

    /** Arranca con las zonas de la precarga (DC-18). */
    public ZonaMemoriaAdapter() {
        this(DatosSantaMarta.zonas());
    }

    public ZonaMemoriaAdapter(List<Zona> zonas) {
        this.zonas = List.copyOf(zonas);
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
