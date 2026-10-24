package co.edu.unimagdalena.samariantour.nucleo.administracion.infraestructura.salida.persistencia;

import co.edu.unimagdalena.samariantour.nucleo.administracion.dominio.modelo.Decision;
import co.edu.unimagdalena.samariantour.nucleo.administracion.dominio.modelo.DecisionModeracion;
import co.edu.unimagdalena.samariantour.nucleo.administracion.dominio.modelo.TipoModerado;
import co.edu.unimagdalena.samariantour.nucleo.administracion.dominio.puerto.salida.DecisionesModeracionRepositoryPort;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

/** El historial de moderación en la tabla {@code decisiones_moderacion} de PostgreSQL (DN-08). */
@Component
public class DecisionesModeracionJpaAdapter implements DecisionesModeracionRepositoryPort {

    private final DecisionModeracionJpaRepository repositorio;

    DecisionesModeracionJpaAdapter(DecisionModeracionJpaRepository repositorio) {
        this.repositorio = repositorio;
    }

    @Override
    public Optional<DecisionModeracion> buscar(TipoModerado tipo, String moderadoId, Integer numeroVersion) {
        var fila = numeroVersion == null
                ? repositorio.findByTipoAndModeradoIdAndNumeroVersionIsNull(tipo.name(), moderadoId)
                : repositorio.findByTipoAndModeradoIdAndNumeroVersion(tipo.name(), moderadoId, numeroVersion);
        return fila.map(DecisionesModeracionJpaAdapter::aDominio);
    }

    @Override
    public boolean registrar(DecisionModeracion decision) {
        return repositorio.insertarSiNoExiste(UUID.randomUUID(), decision.tipo().name(), decision.moderadoId(),
                decision.numeroVersion(), decision.decision().name(), decision.motivo(), decision.administradorId(),
                decision.decididaEn(), decision.idAviso()) == 1;
    }

    private static DecisionModeracion aDominio(DecisionModeracionEntity fila) {
        return new DecisionModeracion(TipoModerado.valueOf(fila.tipo()), fila.moderadoId(), fila.numeroVersion(),
                Decision.valueOf(fila.decision()), fila.motivo(), fila.administradorId(), fila.decididaEn(),
                fila.idAviso());
    }
}
