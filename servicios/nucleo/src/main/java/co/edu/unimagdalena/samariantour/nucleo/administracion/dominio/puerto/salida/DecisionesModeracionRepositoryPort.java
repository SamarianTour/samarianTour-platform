package co.edu.unimagdalena.samariantour.nucleo.administracion.dominio.puerto.salida;

import co.edu.unimagdalena.samariantour.nucleo.administracion.dominio.modelo.DecisionModeracion;
import co.edu.unimagdalena.samariantour.nucleo.administracion.dominio.modelo.TipoModerado;

import java.util.Optional;

/** El historial de las decisiones de moderación: una por versión de oferta y una por opinión (DN-08). */
public interface DecisionesModeracionRepositoryPort {

    /** @param numeroVersion vacío para una opinión */
    Optional<DecisionModeracion> buscar(TipoModerado tipo, String moderadoId, Integer numeroVersion);

    /** @return false si otra decisión sobre lo mismo se guardó antes, por ejemplo desde la otra réplica */
    boolean registrar(DecisionModeracion decision);
}
