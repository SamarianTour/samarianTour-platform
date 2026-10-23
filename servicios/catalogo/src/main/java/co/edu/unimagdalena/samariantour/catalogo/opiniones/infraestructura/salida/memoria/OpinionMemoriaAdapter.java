package co.edu.unimagdalena.samariantour.catalogo.opiniones.infraestructura.salida.memoria;

import co.edu.unimagdalena.samariantour.catalogo.compartido.concurrencia.ConflictoConcurrenciaException;
import co.edu.unimagdalena.samariantour.catalogo.opiniones.dominio.excepcion.OpinionDuplicadaException;
import co.edu.unimagdalena.samariantour.catalogo.opiniones.dominio.modelo.Opinion;
import co.edu.unimagdalena.samariantour.catalogo.opiniones.dominio.modelo.Pagina;
import co.edu.unimagdalena.samariantour.catalogo.opiniones.dominio.modelo.Paginacion;
import co.edu.unimagdalena.samariantour.catalogo.opiniones.dominio.modelo.ResumenCalificaciones;
import co.edu.unimagdalena.samariantour.catalogo.opiniones.dominio.puerto.salida.OpinionRepositoryPort;
import co.edu.unimagdalena.samariantour.catalogo.opiniones.infraestructura.salida.demo.DatosOpiniones;
import org.bson.types.ObjectId;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Predicate;

/**
 * Opiniones en la memoria del proceso, para el perfil {@code memoria}. Guarda y entrega copias, igual que una base de
 * datos, y simula lo que en MongoDB garantizan el índice único {@code {ofertaId, turistaId}} (RF-61) y el bloqueo
 * optimista por revisión (ADR-05). Los métodos son {@code synchronized}: así la verificación de duplicados y el
 * guardado no se intercalan con otra escritura.
 */
@Component
@Profile("memoria")
public class OpinionMemoriaAdapter implements OpinionRepositoryPort {

    /** La más reciente primero y luego por id: el orden de la lectura pública y de "Mis opiniones". */
    private static final Comparator<Opinion> MAS_RECIENTE_PRIMERO = Comparator.comparing(Opinion::fecha).reversed()
            .thenComparing(Opinion::id);
    /** La más antigua primero y luego por id: el orden de la cola de moderación. */
    private static final Comparator<Opinion> MAS_ANTIGUA_PRIMERO = Comparator.comparing(Opinion::fecha)
            .thenComparing(Opinion::id);

    private final Map<String, Opinion> opiniones = new HashMap<>();

    /** Arranca con las opiniones de demostración. */
    public OpinionMemoriaAdapter() {
        this(DatosOpiniones.opiniones());
    }

    /** Guarda las iniciales como cualquier opinión nueva, así que quedan con la revisión siguiente, como en MongoDB. */
    public OpinionMemoriaAdapter(Collection<Opinion> iniciales) {
        iniciales.forEach(this::guardar);
    }

    /** Mismo formato que en MongoDB (DC-30). */
    @Override
    public String nuevoId() {
        return new ObjectId().toHexString();
    }

    @Override
    public synchronized Optional<Opinion> buscarPorId(String id) {
        return Optional.ofNullable(opiniones.get(id)).map(opinion -> copia(opinion, opinion.revision()));
    }

    @Override
    public synchronized void guardar(Opinion opinion) {
        Opinion guardada = opiniones.get(opinion.id());
        long revisionGuardada = guardada == null ? Opinion.REVISION_NUEVA : guardada.revision();
        if (opinion.revision() == Opinion.REVISION_NUEVA && guardada == null
                && existeDe(opinion.turistaId(), opinion.ofertaId())) {
            throw new OpinionDuplicadaException(opinion.turistaId(), opinion.ofertaId());
        }
        if (revisionGuardada != opinion.revision()) {
            throw new ConflictoConcurrenciaException("La opinión %s cambió desde que se leyó".formatted(opinion.id()));
        }
        opiniones.put(opinion.id(), copia(opinion, opinion.revision() + 1));
    }

    @Override
    public synchronized boolean existeDe(UUID turistaId, String ofertaId) {
        return opiniones.values().stream()
                .anyMatch(opinion -> opinion.turistaId().equals(turistaId) && opinion.ofertaId().equals(ofertaId));
    }

    @Override
    public synchronized Pagina<Opinion> buscarAprobadas(String ofertaId, Paginacion paginacion) {
        return paginar(aprobadasDe(ofertaId), MAS_RECIENTE_PRIMERO, paginacion);
    }

    @Override
    public synchronized ResumenCalificaciones resumirAprobadas(String ofertaId) {
        List<Opinion> aprobadas = opiniones.values().stream().filter(aprobadasDe(ofertaId)).toList();
        return new ResumenCalificaciones(aprobadas.size(),
                aprobadas.stream().mapToLong(Opinion::calificacion).sum());
    }

    @Override
    public synchronized Pagina<Opinion> buscarDelTurista(UUID turistaId, Paginacion paginacion) {
        return paginar(opinion -> opinion.turistaId().equals(turistaId), MAS_RECIENTE_PRIMERO, paginacion);
    }

    @Override
    public synchronized Pagina<Opinion> buscarEnModeracion(Paginacion paginacion) {
        return paginar(Opinion::estaEnModeracion, MAS_ANTIGUA_PRIMERO, paginacion);
    }

    @Override
    public synchronized long eliminarDelTurista(UUID turistaId) {
        int antes = opiniones.size();
        opiniones.values().removeIf(opinion -> opinion.turistaId().equals(turistaId));
        return antes - opiniones.size();
    }

    private static Predicate<Opinion> aprobadasDe(String ofertaId) {
        return opinion -> opinion.ofertaId().equals(ofertaId) && opinion.estaAprobada();
    }

    private Pagina<Opinion> paginar(Predicate<Opinion> filtro, Comparator<Opinion> orden, Paginacion paginacion) {
        List<Opinion> todas = opiniones.values().stream().filter(filtro).sorted(orden).toList();
        List<Opinion> pagina = todas.stream()
                .skip(paginacion.desplazamiento())
                .limit(paginacion.tamano())
                .map(opinion -> copia(opinion, opinion.revision()))
                .toList();
        return new Pagina<>(pagina, paginacion, todas.size());
    }

    private static Opinion copia(Opinion opinion, long revision) {
        return Opinion.reconstituir(opinion.id(), opinion.ofertaId(), opinion.turistaId(), opinion.calificacion(),
                opinion.comentario(), opinion.fecha(), opinion.estado(), opinion.motivoRechazo(),
                opinion.revisadaEn(), revision);
    }
}
