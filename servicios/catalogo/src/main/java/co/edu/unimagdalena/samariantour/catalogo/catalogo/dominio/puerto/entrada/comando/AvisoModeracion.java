package co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.puerto.entrada.comando;

import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.excepcion.DatoInvalidoException;
import co.edu.unimagdalena.samariantour.catalogo.catalogo.dominio.modelo.Validaciones;

import java.util.Objects;

/**
 * Decisión del Administrador sobre una versión de una oferta (RF-79).
 *
 * @param numeroVersion versión que el Administrador revisó; si ya no es la que está en moderación, el aviso se ignora
 * @param motivo        obligatorio al rechazar
 */
public record AvisoModeracion(String idAviso, String ofertaId, int numeroVersion, DecisionModeracion decision,
                              String motivo) implements AvisoNucleoCommand {

    public AvisoModeracion {
        idAviso = Validaciones.texto(idAviso, "idAviso");
        ofertaId = Validaciones.texto(ofertaId, "ofertaId");
        Validaciones.positivo(numeroVersion, "numeroVersion");
        Objects.requireNonNull(decision, "decision");
        motivo = Validaciones.textoOpcional(motivo);
        if (decision == DecisionModeracion.RECHAZAR && motivo == null) {
            throw new DatoInvalidoException("motivo", "es obligatorio al rechazar");
        }
    }
}
