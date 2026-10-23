package co.edu.unimagdalena.samariantour.catalogo.opiniones.dominio.puerto.entrada.comando;

import co.edu.unimagdalena.samariantour.catalogo.opiniones.dominio.excepcion.DatoInvalidoException;
import co.edu.unimagdalena.samariantour.catalogo.opiniones.dominio.modelo.Validaciones;

import java.util.Objects;

/**
 * Decisión del administrador sobre una opinión (RF-80, DC-44). No lleva número de versión: una opinión no se edita
 * (DC-42), así que solo puede haber una decisión que aplicar.
 *
 * @param motivo obligatorio al rechazar; el turista lo ve en "Mis opiniones"
 */
public record AvisoModeracionOpinion(String idAviso, String opinionId, DecisionModeracion decision, String motivo)
        implements AvisoOpinionCommand {

    public AvisoModeracionOpinion {
        idAviso = Validaciones.texto(idAviso, "idAviso");
        opinionId = Validaciones.texto(opinionId, "opinionId");
        Objects.requireNonNull(decision, "decision");
        motivo = Validaciones.textoOpcional(motivo);
        if (decision == DecisionModeracion.RECHAZAR && motivo == null) {
            throw new DatoInvalidoException("motivo", "es obligatorio al rechazar");
        }
    }
}
