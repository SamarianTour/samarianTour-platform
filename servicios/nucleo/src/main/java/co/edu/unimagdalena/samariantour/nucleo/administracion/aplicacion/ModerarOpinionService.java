package co.edu.unimagdalena.samariantour.nucleo.administracion.aplicacion;

import co.edu.unimagdalena.samariantour.nucleo.administracion.dominio.excepcion.NoEstaEnModeracionException;
import co.edu.unimagdalena.samariantour.nucleo.administracion.dominio.modelo.Decision;
import co.edu.unimagdalena.samariantour.nucleo.administracion.dominio.modelo.DecisionModeracion;
import co.edu.unimagdalena.samariantour.nucleo.administracion.dominio.modelo.OpinionEnModeracion;
import co.edu.unimagdalena.samariantour.nucleo.administracion.dominio.modelo.TipoModerado;
import co.edu.unimagdalena.samariantour.nucleo.administracion.dominio.puerto.entrada.ModerarOpinionUseCase;
import co.edu.unimagdalena.samariantour.nucleo.administracion.dominio.puerto.entrada.comando.ModerarOpinionCommand;
import co.edu.unimagdalena.samariantour.nucleo.administracion.dominio.puerto.salida.AvisosModeracionPort;
import co.edu.unimagdalena.samariantour.nucleo.administracion.dominio.puerto.salida.DecisionesModeracionRepositoryPort;
import co.edu.unimagdalena.samariantour.nucleo.administracion.dominio.puerto.salida.ModeracionCatalogoPort;
import co.edu.unimagdalena.samariantour.nucleo.administracion.dominio.puerto.salida.NotificacionPort;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;

/** Decide sobre una opinión en moderación (RF-80, DN-08), con las mismas reglas que las ofertas, sin versiones. */
public class ModerarOpinionService implements ModerarOpinionUseCase {

    private final DecisionesModeracionRepositoryPort decisiones;
    private final ModeracionCatalogoPort catalogo;
    private final AvisosModeracionPort avisos;
    private final NotificacionPort notificacion;
    private final Clock reloj;

    public ModerarOpinionService(DecisionesModeracionRepositoryPort decisiones, ModeracionCatalogoPort catalogo,
                                 AvisosModeracionPort avisos, NotificacionPort notificacion, Clock reloj) {
        this.decisiones = decisiones;
        this.catalogo = catalogo;
        this.avisos = avisos;
        this.notificacion = notificacion;
        this.reloj = reloj;
    }

    @Override
    @Transactional
    public DecisionModeracion moderar(ModerarOpinionCommand comando) {
        DecisionModeracion nueva = DecisionModeracion.deOpinion(comando.opinionId(), comando.decision(),
                comando.motivo(), comando.administradorId(), reloj.instant());
        var previa = decisiones.buscar(TipoModerado.OPINION, comando.opinionId(), null);
        if (previa.isPresent()) {
            return ModerarOfertaService.yaDecidida(previa.get(), nueva);
        }
        OpinionEnModeracion opinion = catalogo.opinionEnModeracion(comando.opinionId())
                .orElseThrow(() -> new NoEstaEnModeracionException("La opinión " + comando.opinionId()));
        if (!decisiones.registrar(nueva)) {
            return ModerarOfertaService.yaDecidida(
                    decisiones.buscar(TipoModerado.OPINION, comando.opinionId(), null).orElseThrow(), nueva);
        }
        avisos.avisar(nueva);
        if (nueva.decision() == Decision.RECHAZAR) {
            notificacion.rechazoDeOpinion(opinion.turistaId(), nueva.motivo());
        }
        return nueva;
    }
}
