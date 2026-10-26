package co.edu.unimagdalena.samariantour.nucleo.solicitudes.infraestructura.salida.persistencia;

import co.edu.unimagdalena.samariantour.nucleo.compartido.concurrencia.ConflictoConcurrenciaException;
import co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.excepcion.SolicitudDuplicadaException;
import co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.modelo.ActorTransicion;
import co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.modelo.CamposDelTipo;
import co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.modelo.CondicionesOfrecidas;
import co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.modelo.DatosOfertaCopiados;
import co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.modelo.EstadoSolicitud;
import co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.modelo.Pagina;
import co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.modelo.Paginacion;
import co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.modelo.SolicitudDisponibilidad;
import co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.modelo.TipoOferta;
import co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.modelo.TransicionSolicitud;
import co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.puerto.salida.SolicitudRepositoryPort;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Las solicitudes en las tablas {@code solicitudes} y {@code transiciones_solicitud} de PostgreSQL (DN-02, DN-11). El
 * historial solo agrega filas: al actualizar se insertan las transiciones que el dominio agregó desde que la leyó.
 */
@Component
public class SolicitudJpaAdapter implements SolicitudRepositoryPort {

    /** El índice único parcial de ReglaNoDuplicado (V3). */
    static final String INDICE_NO_DUPLICADO = "solicitudes_no_duplicada";

    private static final List<String> ABIERTAS = Arrays.stream(EstadoSolicitud.values())
            .filter(estado -> !estado.esTerminal()).map(Enum::name).toList();

    private final SolicitudJpaRepository solicitudes;
    private final TransicionSolicitudJpaRepository transiciones;

    SolicitudJpaAdapter(SolicitudJpaRepository solicitudes, TransicionSolicitudJpaRepository transiciones) {
        this.solicitudes = solicitudes;
        this.transiciones = transiciones;
    }

    @Override
    public void insertar(SolicitudDisponibilidad solicitud) {
        SolicitudEntity fila = new SolicitudEntity(solicitud.id(), solicitud.turistaId(), solicitud.ofertaId(),
                solicitud.oferta().tipo().name(), solicitud.oferta().nombre(), solicitud.oferta().prestadorId(),
                solicitud.oferta().zonaId(), solicitud.fechaInicio(), solicitud.horaSalida(), solicitud.horaReserva(),
                solicitud.fechaSalida(), solicitud.numeroUnidades(), solicitud.personas(), solicitud.notaTurista(),
                solicitud.operadorId(), solicitud.creadaEn());
        copiarEstado(solicitud, fila);
        try {
            solicitudes.saveAndFlush(fila);
        } catch (DataIntegrityViolationException e) {
            if (String.valueOf(e.getMostSpecificCause().getMessage()).contains(INDICE_NO_DUPLICADO)) {
                throw new SolicitudDuplicadaException();
            }
            throw e;
        }
        agregarTransiciones(solicitud, 0);
    }

    @Override
    public void actualizar(SolicitudDisponibilidad solicitud) {
        SolicitudEntity fila = solicitudes.findById(solicitud.id())
                .orElseThrow(() -> new ConflictoConcurrenciaException(
                        "La solicitud %s ya no existe".formatted(solicitud.id())));
        if (!Objects.equals(fila.revision(), solicitud.revision())) {
            throw new ConflictoConcurrenciaException(
                    "La solicitud %s cambió desde que se leyó".formatted(solicitud.id()));
        }
        copiarEstado(solicitud, fila);
        try {
            solicitudes.flush();
        } catch (OptimisticLockingFailureException e) {
            throw new ConflictoConcurrenciaException("La solicitud %s cambió mientras se guardaba"
                    .formatted(solicitud.id()));
        }
        agregarTransiciones(solicitud, (int) transiciones.countBySolicitudId(solicitud.id()));
    }

    @Override
    public Optional<SolicitudDisponibilidad> buscar(UUID id) {
        return solicitudes.findById(id).map(fila -> conHistorial(List.of(fila)).getFirst());
    }

    @Override
    public List<SolicitudDisponibilidad> abiertasDelTurista(UUID turistaId, String ofertaId, LocalDate fechaInicio) {
        return conHistorial(solicitudes.findByTuristaIdAndOfertaIdAndFechaInicioAndEstadoIn(turistaId, ofertaId,
                fechaInicio, ABIERTAS));
    }

    @Override
    public Pagina<SolicitudDisponibilidad> delTurista(UUID turistaId, Paginacion paginacion) {
        return pagina(solicitudes.findByTuristaIdOrderByCreadaEnDescIdAsc(turistaId, pedido(paginacion)), paginacion);
    }

    @Override
    public Pagina<SolicitudDisponibilidad> delOperador(UUID operadorId, EstadoSolicitud estado,
                                                       Paginacion paginacion) {
        Page<SolicitudEntity> filas = estado == EstadoSolicitud.PENDIENTE
                ? solicitudes.findByOperadorIdAndEstadoOrderByCreadaEnAscIdAsc(operadorId, estado.name(),
                pedido(paginacion))
                : solicitudes.findByOperadorIdAndEstadoOrderByCreadaEnDescIdAsc(operadorId, estado.name(),
                pedido(paginacion));
        return pagina(filas, paginacion);
    }

    @Override
    public List<SolicitudDisponibilidad> tomarPorVencer(Instant ahora, int limite) {
        return conHistorial(solicitudes.tomarPorVencer(ahora, limite));
    }

    private static void copiarEstado(SolicitudDisponibilidad solicitud, SolicitudEntity fila) {
        CondicionesOfrecidas condiciones = solicitud.condiciones();
        fila.cambiarEstado(solicitud.estado().name(), condiciones == null ? null : condiciones.precioBase(),
                condiciones == null ? null : condiciones.politicaCancelacion(),
                condiciones == null ? null : condiciones.nota(), solicitud.aparteHasta(), solicitud.venceEn(),
                solicitud.limiteVencimiento(), solicitud.intentoPagoEnCurso(), solicitud.intentosPagoFallidos(),
                solicitud.actualizadaEn());
    }

    /** Inserta las transiciones del historial desde la posición {@code desde}: las que aún no están guardadas. */
    private void agregarTransiciones(SolicitudDisponibilidad solicitud, int desde) {
        List<TransicionSolicitud> historial = solicitud.historial();
        for (int orden = desde; orden < historial.size(); orden++) {
            TransicionSolicitud transicion = historial.get(orden);
            transiciones.save(new TransicionSolicitudEntity(UUID.randomUUID(), solicitud.id(), orden,
                    transicion.de() == null ? null : transicion.de().name(), transicion.a().name(), transicion.en(),
                    transicion.actor().name(), transicion.actorId(), transicion.motivo()));
        }
        transiciones.flush();
    }

    private static PageRequest pedido(Paginacion paginacion) {
        return PageRequest.of(paginacion.pagina(), paginacion.tamano());
    }

    private Pagina<SolicitudDisponibilidad> pagina(Page<SolicitudEntity> filas, Paginacion paginacion) {
        return new Pagina<>(conHistorial(filas.getContent()), paginacion.pagina(), paginacion.tamano(),
                filas.getTotalElements());
    }

    /** Arma las solicitudes con su historial, con una sola consulta para todas. */
    private List<SolicitudDisponibilidad> conHistorial(List<SolicitudEntity> filas) {
        if (filas.isEmpty()) {
            return List.of();
        }
        Map<UUID, List<TransicionSolicitud>> historiales = transiciones
                .findBySolicitudIdInOrderBySolicitudIdAscOrdenAsc(filas.stream().map(SolicitudEntity::id).toList())
                .stream()
                .collect(Collectors.groupingBy(TransicionSolicitudEntity::solicitudId,
                        Collectors.mapping(SolicitudJpaAdapter::aDominio, Collectors.toList())));
        return filas.stream().map(fila -> aDominio(fila, historiales.getOrDefault(fila.id(), List.of()))).toList();
    }

    private static TransicionSolicitud aDominio(TransicionSolicitudEntity fila) {
        EstadoSolicitud origen = fila.estadoOrigen() == null ? null : EstadoSolicitud.valueOf(fila.estadoOrigen());
        return new TransicionSolicitud(origen, EstadoSolicitud.valueOf(fila.estadoDestino()), fila.ocurridaEn(),
                ActorTransicion.valueOf(fila.actor()), fila.actorId(), fila.motivo());
    }

    private static SolicitudDisponibilidad aDominio(SolicitudEntity fila, List<TransicionSolicitud> historial) {
        CondicionesOfrecidas condiciones = fila.precioBase() == null ? null
                : new CondicionesOfrecidas(fila.precioBase(), fila.politicaCancelacion(), fila.notaOperador());
        return SolicitudDisponibilidad.reconstituir(fila.id(), fila.turistaId(), fila.ofertaId(),
                new DatosOfertaCopiados(TipoOferta.valueOf(fila.tipoOferta()), fila.nombreOferta(),
                        fila.prestadorId(), fila.zonaId()),
                fila.fechaInicio(), new CamposDelTipo(fila.horaSalida(), fila.horaReserva(), fila.fechaSalida(),
                        fila.numeroUnidades()), fila.personas(), fila.notaTurista(), fila.operadorId(),
                fila.creadaEn(), EstadoSolicitud.valueOf(fila.estado()), condiciones, fila.aparteHasta(),
                fila.venceEn(), fila.intentoPagoEnCurso(), fila.intentosPagoFallidos(), fila.actualizadaEn(),
                fila.revision(), historial);
    }
}
