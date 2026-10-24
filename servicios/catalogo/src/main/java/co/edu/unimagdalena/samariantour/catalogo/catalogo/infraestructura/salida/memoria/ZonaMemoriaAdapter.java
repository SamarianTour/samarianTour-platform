package co.edu.unimagdalena.samariantour.catalogo.catalogo.infraestructura.salida.memoria;

import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.excepcion.ZonaDuplicadaException;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.Zona;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.puerto.salida.ZonaRepositoryPort;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.infraestructura.salida.demo.DatosSantaMarta;
import org.bson.types.ObjectId;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Zonas en la memoria del proceso (DC-49). Comprobar el nombre y guardar van juntos, así que dos peticiones a la vez no
 * dejan dos zonas con el mismo nombre.
 */
@Component
@Profile("memoria")
public class ZonaMemoriaAdapter implements ZonaRepositoryPort {

    private final Map<String, Zona> zonas = new LinkedHashMap<>();

    /** Arranca con las zonas de la precarga (DC-18). */
    public ZonaMemoriaAdapter() {
        this(DatosSantaMarta.zonas());
    }

    public ZonaMemoriaAdapter(List<Zona> zonas) {
        zonas.forEach(zona -> this.zonas.put(zona.id(), zona));
    }

    @Override
    public synchronized Optional<Zona> buscarPorId(String id) {
        return Optional.ofNullable(zonas.get(id));
    }

    @Override
    public synchronized List<Zona> buscarActivas() {
        return buscarTodas().stream().filter(Zona::activa).toList();
    }

    @Override
    public synchronized List<Zona> buscarTodas() {
        return zonas.values().stream().sorted(Comparator.comparing(Zona::nombre)).toList();
    }

    @Override
    public String nuevoId() {
        return new ObjectId().toHexString();
    }

    @Override
    public synchronized void guardar(Zona zona) {
        boolean repetido = zonas.values().stream()
                .anyMatch(otra -> !otra.id().equals(zona.id()) && otra.claveNombre().equals(zona.claveNombre()));
        if (repetido) {
            throw new ZonaDuplicadaException(zona.nombre());
        }
        zonas.put(zona.id(), zona);
    }
}
