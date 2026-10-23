package co.edu.unimagdalena.samariantour.catalogo.opiniones.infraestructura.salida.memoria;

import co.edu.unimagdalena.samariantour.catalogo.opiniones.dominio.modelo.HabilitacionOpinion;
import co.edu.unimagdalena.samariantour.catalogo.opiniones.dominio.puerto.salida.HabilitacionOpinionRepositoryPort;
import co.edu.unimagdalena.samariantour.catalogo.opiniones.infraestructura.salida.demo.DatosOpiniones;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Habilitaciones para opinar en la memoria del proceso, con los datos de demostración. El conjunto garantiza una sola
 * habilitación por turista y oferta, como el índice único de MongoDB.
 */
@Component
@Profile("memoria")
public class HabilitacionOpinionMemoriaAdapter implements HabilitacionOpinionRepositoryPort {

    private final Set<HabilitacionOpinion> habilitaciones = ConcurrentHashMap.newKeySet();

    public HabilitacionOpinionMemoriaAdapter() {
        this(DatosOpiniones.habilitaciones());
    }

    public HabilitacionOpinionMemoriaAdapter(Collection<HabilitacionOpinion> iniciales) {
        habilitaciones.addAll(iniciales);
    }

    @Override
    public boolean registrar(HabilitacionOpinion habilitacion) {
        return habilitaciones.add(habilitacion);
    }

    @Override
    public boolean existe(UUID turistaId, String ofertaId) {
        return habilitaciones.contains(new HabilitacionOpinion(turistaId, ofertaId));
    }

    @Override
    public long eliminarDelTurista(UUID turistaId) {
        int antes = habilitaciones.size();
        habilitaciones.removeIf(habilitacion -> habilitacion.turistaId().equals(turistaId));
        return antes - habilitaciones.size();
    }
}
