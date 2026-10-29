package co.edu.unimagdalena.samariantour.nucleo.administracion.aplicacion;

import co.edu.unimagdalena.samariantour.nucleo.administracion.dominio.excepcion.CatalogoNoDisponibleException;
import co.edu.unimagdalena.samariantour.nucleo.administracion.dominio.modelo.DecisionModeracion;
import co.edu.unimagdalena.samariantour.nucleo.administracion.dominio.modelo.OfertaEnModeracion;
import co.edu.unimagdalena.samariantour.nucleo.administracion.dominio.modelo.OpinionEnModeracion;
import co.edu.unimagdalena.samariantour.nucleo.administracion.dominio.modelo.TipoModerado;
import co.edu.unimagdalena.samariantour.nucleo.administracion.dominio.puerto.salida.DecisionesModeracionRepositoryPort;
import co.edu.unimagdalena.samariantour.nucleo.administracion.dominio.puerto.salida.ModeracionCatalogoPort;
import co.edu.unimagdalena.samariantour.nucleo.administracion.dominio.puerto.salida.NotificacionPort;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/** Los servicios de moderación con dobles en memoria del catálogo, el historial, la bandeja y las notificaciones. */
class EscenarioModeracion {

    static final UUID ADMINISTRADOR = UUID.fromString("60000000-0000-0000-0000-000000000006");
    static final UUID PRESTADOR = UUID.fromString("20000000-0000-0000-0000-000000000002");
    static final UUID TURISTA = UUID.fromString("80000000-0000-0000-0000-000000000008");
    static final Instant AHORA = Instant.parse("2026-10-24T15:00:00Z");

    final Map<String, OfertaEnModeracion> ofertas = new HashMap<>();
    final Map<String, OpinionEnModeracion> opiniones = new HashMap<>();
    boolean catalogoCaido;
    final List<DecisionModeracion> historial = new ArrayList<>();
    final List<DecisionModeracion> avisos = new ArrayList<>();
    final List<String> notificaciones = new ArrayList<>();

    private final ModeracionCatalogoPort catalogo = new ModeracionCatalogoPort() {
        @Override
        public Optional<OfertaEnModeracion> ofertaEnModeracion(String ofertaId) {
            disponible();
            return Optional.ofNullable(ofertas.get(ofertaId));
        }

        @Override
        public Optional<OpinionEnModeracion> opinionEnModeracion(String opinionId) {
            disponible();
            return Optional.ofNullable(opiniones.get(opinionId));
        }
    };

    private final DecisionesModeracionRepositoryPort decisiones = new DecisionesModeracionRepositoryPort() {
        @Override
        public Optional<DecisionModeracion> buscar(TipoModerado tipo, String moderadoId, Integer numeroVersion) {
            return historial.stream().filter(decision -> decision.tipo() == tipo
                    && decision.moderadoId().equals(moderadoId)
                    && Objects.equals(decision.numeroVersion(), numeroVersion)).findFirst();
        }

        @Override
        public boolean registrar(DecisionModeracion decision) {
            if (buscar(decision.tipo(), decision.moderadoId(), decision.numeroVersion()).isPresent()) {
                return false;
            }
            return historial.add(decision);
        }
    };

    private final NotificacionPort notificacion = new NotificacionPort() {
        @Override
        public void rechazoDeOferta(UUID prestadorId, String nombreOferta, String motivo) {
            notificaciones.add(prestadorId + " " + nombreOferta + ": " + motivo);
        }

        @Override
        public void rechazoDeOpinion(UUID turistaId, String motivo) {
            notificaciones.add(turistaId + ": " + motivo);
        }
    };

    /** Los prestadores con el RNT inactivo: sus ofertas no se aprueban (DN-35). */
    final Set<UUID> inhabilitados = new HashSet<>();

    final ModerarOfertaService moderarOferta = new ModerarOfertaService(decisiones, catalogo, avisos::add,
            notificacion, prestadorId -> !inhabilitados.contains(prestadorId), Clock.fixed(AHORA, ZoneOffset.UTC));
    final ModerarOpinionService moderarOpinion = new ModerarOpinionService(decisiones, catalogo, avisos::add,
            notificacion, Clock.fixed(AHORA, ZoneOffset.UTC));

    private void disponible() {
        if (catalogoCaido) {
            throw new CatalogoNoDisponibleException("no respondió a tiempo");
        }
    }
}
