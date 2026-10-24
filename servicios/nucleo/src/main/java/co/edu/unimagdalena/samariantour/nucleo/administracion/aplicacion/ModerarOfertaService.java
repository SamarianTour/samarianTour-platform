package co.edu.unimagdalena.samariantour.nucleo.administracion.aplicacion;

import co.edu.unimagdalena.samariantour.nucleo.administracion.dominio.excepcion.ModeracionYaDecididaException;
import co.edu.unimagdalena.samariantour.nucleo.administracion.dominio.excepcion.NoEstaEnModeracionException;
import co.edu.unimagdalena.samariantour.nucleo.administracion.dominio.excepcion.VersionNoVigenteException;
import co.edu.unimagdalena.samariantour.nucleo.administracion.dominio.modelo.Decision;
import co.edu.unimagdalena.samariantour.nucleo.administracion.dominio.modelo.DecisionModeracion;
import co.edu.unimagdalena.samariantour.nucleo.administracion.dominio.modelo.OfertaEnModeracion;
import co.edu.unimagdalena.samariantour.nucleo.administracion.dominio.modelo.TipoModerado;
import co.edu.unimagdalena.samariantour.nucleo.administracion.dominio.puerto.entrada.ModerarOfertaUseCase;
import co.edu.unimagdalena.samariantour.nucleo.administracion.dominio.puerto.entrada.comando.ModerarOfertaCommand;
import co.edu.unimagdalena.samariantour.nucleo.administracion.dominio.puerto.salida.AvisosModeracionPort;
import co.edu.unimagdalena.samariantour.nucleo.administracion.dominio.puerto.salida.DecisionesModeracionRepositoryPort;
import co.edu.unimagdalena.samariantour.nucleo.administracion.dominio.puerto.salida.ModeracionCatalogoPort;
import co.edu.unimagdalena.samariantour.nucleo.administracion.dominio.puerto.salida.NotificacionPort;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;

/**
 * Decide sobre la versión en moderación de una oferta (RF-79, DN-08). Primero mira el historial: repetir la misma
 * decisión devuelve la de entonces aunque el catálogo ya la haya aplicado. Después verifica con el catálogo que esa
 * versión siga en moderación, y guarda la decisión y su aviso en una sola transacción.
 */
public class ModerarOfertaService implements ModerarOfertaUseCase {

    private final DecisionesModeracionRepositoryPort decisiones;
    private final ModeracionCatalogoPort catalogo;
    private final AvisosModeracionPort avisos;
    private final NotificacionPort notificacion;
    private final Clock reloj;

    public ModerarOfertaService(DecisionesModeracionRepositoryPort decisiones, ModeracionCatalogoPort catalogo,
                                AvisosModeracionPort avisos, NotificacionPort notificacion, Clock reloj) {
        this.decisiones = decisiones;
        this.catalogo = catalogo;
        this.avisos = avisos;
        this.notificacion = notificacion;
        this.reloj = reloj;
    }

    @Override
    @Transactional
    public DecisionModeracion moderar(ModerarOfertaCommand comando) {
        DecisionModeracion nueva = DecisionModeracion.deOferta(comando.ofertaId(), comando.numeroVersion(),
                comando.decision(), comando.motivo(), comando.administradorId(), reloj.instant());
        var previa = decisiones.buscar(TipoModerado.OFERTA, comando.ofertaId(), comando.numeroVersion());
        if (previa.isPresent()) {
            return yaDecidida(previa.get(), nueva);
        }
        OfertaEnModeracion oferta = catalogo.ofertaEnModeracion(comando.ofertaId())
                .orElseThrow(() -> new NoEstaEnModeracionException("La oferta " + comando.ofertaId()));
        if (oferta.numeroVersion() != comando.numeroVersion()) {
            throw new VersionNoVigenteException(comando.ofertaId(), comando.numeroVersion(), oferta.numeroVersion());
        }
        if (!decisiones.registrar(nueva)) {
            // Otro administrador, quizá en la otra réplica, decidió lo mismo un instante antes.
            return yaDecidida(decisiones.buscar(TipoModerado.OFERTA, comando.ofertaId(), comando.numeroVersion())
                    .orElseThrow(), nueva);
        }
        avisos.avisar(nueva);
        if (nueva.decision() == Decision.RECHAZAR) {
            notificacion.rechazoDeOferta(oferta.prestadorId(), oferta.nombre(), nueva.motivo());
        }
        return nueva;
    }

    static DecisionModeracion yaDecidida(DecisionModeracion previa, DecisionModeracion nueva) {
        if (!previa.coincideCon(nueva)) {
            throw new ModeracionYaDecididaException(descripcion(previa), previa.decision());
        }
        return previa;
    }

    private static String descripcion(DecisionModeracion decision) {
        return decision.numeroVersion() == null
                ? "La opinión " + decision.moderadoId()
                : "La versión %d de la oferta %s".formatted(decision.numeroVersion(), decision.moderadoId());
    }
}
