package co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.modelo;

import co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.excepcion.DatoInvalidoException;
import co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.excepcion.PlazoVencidoException;
import co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.excepcion.SinTiempoParaConfirmarException;
import co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.excepcion.TransicionInvalidaException;
import co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.politica.PoliticaAnticipacion;
import co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.politica.PoliticaVentanas;
import co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.politica.ReglaNotificaciones;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import static co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.modelo.EstadoSolicitud.ATENDIDA;
import static co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.modelo.EstadoSolicitud.CONFIRMADA;
import static co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.modelo.EstadoSolicitud.DECLINADA;
import static co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.modelo.EstadoSolicitud.NO_DISPONIBLE;
import static co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.modelo.EstadoSolicitud.PAGADA;
import static co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.modelo.EstadoSolicitud.PAGO_FALLIDO;
import static co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.modelo.EstadoSolicitud.PENDIENTE;
import static co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.modelo.EstadoSolicitud.SIN_RESPUESTA;
import static co.edu.unimagdalena.samariantour.nucleo.solicitudes.dominio.modelo.EstadoSolicitud.VENCIDA;

/**
 * Una solicitud de disponibilidad desde que el turista la envía hasta que se cierra (RF-30 a RF-45, RF-50, RF-55,
 * RF-58, DN-09). Las transiciones válidas viven aquí; cualquier otra lanza {@link TransicionInvalidaException}:
 * <ul>
 *   <li>PENDIENTE → ATENDIDA (operador, con condiciones) | NO_DISPONIBLE (operador) | SIN_RESPUESTA (sistema,
 *   24 h);</li>
 *   <li>ATENDIDA → CONFIRMADA | DECLINADA (turista) | VENCIDA (sistema);</li>
 *   <li>CONFIRMADA → PAGADA | PAGO_FALLIDO (pagos) | VENCIDA (sistema);</li>
 *   <li>PAGO_FALLIDO → PAGADA | PAGO_FALLIDO (otro intento rechazado) | VENCIDA (sistema o tercer rechazo).</li>
 * </ul>
 * Los plazos los calcula {@link PoliticaVentanas}. Cada cambio deja su transición en el historial y las notificaciones
 * que le tocan ({@link ReglaNotificaciones}), que la aplicación guarda en la misma transacción.
 */
public final class SolicitudDisponibilidad {

    public static final int LONGITUD_MAXIMA_NOTA = 500;
    public static final int LONGITUD_MAXIMA_MOTIVO = 500;
    /** Tres intentos de pago en total: dos reintentos después del primero (RF-58). */
    public static final int INTENTOS_PAGO = 3;

    private static final Map<EstadoSolicitud, Set<EstadoSolicitud>> TRANSICIONES = new EnumMap<>(Map.of(
            PENDIENTE, EnumSet.of(ATENDIDA, NO_DISPONIBLE, SIN_RESPUESTA),
            ATENDIDA, EnumSet.of(CONFIRMADA, DECLINADA, VENCIDA),
            CONFIRMADA, EnumSet.of(PAGADA, PAGO_FALLIDO, VENCIDA),
            PAGO_FALLIDO, EnumSet.of(PAGADA, PAGO_FALLIDO, VENCIDA)));

    private final UUID id;
    private final UUID turistaId;
    private final String ofertaId;
    private final DatosOfertaCopiados oferta;
    private final LocalDate fechaInicio;
    private final CamposDelTipo campos;
    private final int personas;
    private final String notaTurista;
    private final UUID operadorId;
    private final Instant creadaEn;
    private EstadoSolicitud estado;
    private CondicionesOfrecidas condiciones;
    private Instant aparteHasta;
    private Instant venceEn;
    private boolean intentoPagoEnCurso;
    private int intentosPagoFallidos;
    private Instant actualizadaEn;
    private final Long revision;
    private final List<TransicionSolicitud> historial;
    private final List<NotificacionSolicitud> notificaciones = new ArrayList<>();
    /** Cuántos cambios se le hicieron desde que se creó o se cargó; no se guarda. */
    private int cambios;

    private SolicitudDisponibilidad(UUID id, UUID turistaId, String ofertaId, DatosOfertaCopiados oferta,
                                    LocalDate fechaInicio, CamposDelTipo campos, int personas, String notaTurista,
                                    UUID operadorId, Instant creadaEn, EstadoSolicitud estado,
                                    CondicionesOfrecidas condiciones, Instant aparteHasta, Instant venceEn,
                                    boolean intentoPagoEnCurso, int intentosPagoFallidos, Instant actualizadaEn,
                                    Long revision,
                                    List<TransicionSolicitud> historial) {
        this.id = Objects.requireNonNull(id, "id");
        this.turistaId = Objects.requireNonNull(turistaId, "turistaId");
        this.ofertaId = Objects.requireNonNull(ofertaId, "ofertaId");
        this.oferta = Objects.requireNonNull(oferta, "oferta");
        this.fechaInicio = Objects.requireNonNull(fechaInicio, "fechaInicio");
        this.campos = Objects.requireNonNull(campos, "campos");
        this.personas = personas;
        this.notaTurista = notaTurista;
        this.operadorId = Objects.requireNonNull(operadorId, "operadorId");
        this.creadaEn = micros(Objects.requireNonNull(creadaEn, "creadaEn"));
        this.estado = Objects.requireNonNull(estado, "estado");
        this.condiciones = condiciones;
        this.aparteHasta = micros(aparteHasta);
        this.venceEn = micros(venceEn);
        this.intentoPagoEnCurso = intentoPagoEnCurso;
        this.intentosPagoFallidos = intentosPagoFallidos;
        this.actualizadaEn = micros(Objects.requireNonNull(actualizadaEn, "actualizadaEn"));
        this.revision = revision;
        this.historial = new ArrayList<>(historial);
    }

    /**
     * El turista envía una solicitud sobre una oferta visible (RF-30 a RF-32). Queda Pendiente, asignada al operador
     * del prestador, con 24 h para atenderla.
     *
     * @param campos     los propios del tipo de oferta: horario de salida, fecha de salida y unidades, u hora de la
     *                   reserva
     * @param operadorId el operador vinculado al prestador de la oferta (RF-32)
     */
    public static SolicitudDisponibilidad crear(UUID id, UUID turistaId, OfertaSolicitable oferta,
                                                LocalDate fechaInicio, CamposDelTipo campos, int personas,
                                                String nota, UUID operadorId, Instant ahora) {
        if (fechaInicio == null) {
            throw new DatoInvalidoException("fechaInicio", "es obligatoria");
        }
        if (!PoliticaAnticipacion.permite(fechaInicio, ahora)) {
            throw new DatoInvalidoException("fechaInicio", "debe ser desde el %s: al menos %d días calendario "
                    .formatted(PoliticaAnticipacion.primeraFechaPermitida(ahora), PoliticaAnticipacion.DIAS_MINIMOS)
                    + "después del envío (RF-30)");
        }
        Objects.requireNonNull(campos, "campos").verificar(oferta, fechaInicio);
        if (personas < 1 || personas > oferta.capacidadMaxima()) {
            throw new DatoInvalidoException("personas",
                    "debe estar entre 1 y %d, la capacidad de la oferta".formatted(oferta.capacidadMaxima()));
        }
        String limpia = Textos.limpio(nota);
        Textos.verificarLongitud("nota", limpia, LONGITUD_MAXIMA_NOTA);
        SolicitudDisponibilidad solicitud = new SolicitudDisponibilidad(id, turistaId, oferta.id(), oferta.datos(),
                fechaInicio, campos, personas, limpia, operadorId, ahora, PENDIENTE, null, null,
                PoliticaVentanas.plazoOperador(ahora), false, 0, ahora, null, List.of());
        solicitud.registrar(new TransicionSolicitud(null, PENDIENTE, ahora, ActorTransicion.TURISTA, turistaId,
                null));
        return solicitud;
    }

    /** Vuelve a armar una solicitud guardada, sin validar ni notificar. */
    public static SolicitudDisponibilidad reconstituir(UUID id, UUID turistaId, String ofertaId,
                                                       DatosOfertaCopiados oferta, LocalDate fechaInicio,
                                                       CamposDelTipo campos, int personas, String notaTurista,
                                                       UUID operadorId, Instant creadaEn, EstadoSolicitud estado,
                                                       CondicionesOfrecidas condiciones, Instant aparteHasta,
                                                       Instant venceEn,
                                                       boolean intentoPagoEnCurso, int intentosPagoFallidos,
                                                       Instant actualizadaEn, long revision,
                                                       List<TransicionSolicitud> historial) {
        return new SolicitudDisponibilidad(id, turistaId, ofertaId, oferta, fechaInicio, campos, personas,
                notaTurista, operadorId, creadaEn, estado, condiciones, aparteHasta, venceEn, intentoPagoEnCurso,
                intentosPagoFallidos, actualizadaEn, revision, historial);
    }

    // ---- Operador (RF-37 a RF-40)

    /**
     * El operador registra el precio base y las condiciones que transmitió el prestador, y el aparte del cupo que
     * acordó con él (RF-37 a RF-39). Abre la ventana de confirmación y pago (RF-55), que es también la vigencia del
     * aparte (RF-38). No se puede si la ventana, con su gracia, terminaría después del inicio del servicio: entonces
     * solo queda No disponible (RF-39).
     */
    public void atender(UUID operadorId, CondicionesOfrecidas condiciones, Instant ahora) {
        Objects.requireNonNull(condiciones, "condiciones");
        exigirTransicion(ATENDIDA);
        exigirDentroDelPlazo(ahora);
        Instant ventana = PoliticaVentanas.ventanaConfirmacion(ahora);
        Instant finConGracia = PoliticaVentanas.conGracia(ventana);
        if (finConGracia.isAfter(inicioServicio())) {
            throw new SinTiempoParaConfirmarException(finConGracia, inicioServicio());
        }
        this.condiciones = condiciones;
        this.aparteHasta = micros(ventana);
        this.venceEn = micros(ventana);
        transicionar(ATENDIDA, ActorTransicion.OPERADOR, operadorId, null, ahora);
    }

    /** El prestador no tiene cupo (RF-39). El motivo es opcional y nunca el detalle de la negociación (RF-40). */
    public void marcarNoDisponible(UUID operadorId, String motivo, Instant ahora) {
        exigirTransicion(NO_DISPONIBLE);
        exigirDentroDelPlazo(ahora);
        String limpio = Textos.limpio(motivo);
        Textos.verificarLongitud("nota", limpio, LONGITUD_MAXIMA_MOTIVO);
        this.venceEn = null;
        transicionar(NO_DISPONIBLE, ActorTransicion.OPERADOR, operadorId, limpio, ahora);
    }

    // ---- Turista (RF-42, RF-43)

    /** El turista confirma (pasa a pagar) o declina (se cierra y el cupo se libera), dentro de la ventana (RF-43). */
    public void responder(UUID turistaId, DecisionTurista decision, Instant ahora) {
        Objects.requireNonNull(decision, "decision");
        EstadoSolicitud destino = decision == DecisionTurista.CONFIRMAR ? CONFIRMADA : DECLINADA;
        exigirTransicion(destino);
        exigirDentroDelPlazo(ahora);
        if (destino == DECLINADA) {
            this.venceEn = null;
        }
        transicionar(destino, ActorTransicion.TURISTA, turistaId, null, ahora);
    }

    // ---- Sistema (RF-41, RF-55)

    /**
     * Aplica el vencimiento si ya toca: una Pendiente pasa a Sin respuesta a las 24 h (RF-41); una Atendida, Confirmada
     * o con el pago fallido pasa a Vencida al cerrar la ventana, o hasta 30 min después si hay un intento de pago en
     * curso (RF-55).
     *
     * @return false si todavía no toca o la solicitud ya está cerrada
     */
    public boolean vencer(Instant ahora) {
        Instant limite = limiteVencimiento();
        if (limite == null || ahora.isBefore(limite)) {
            return false;
        }
        if (estado == PENDIENTE) {
            this.venceEn = null;
            transicionar(SIN_RESPUESTA, ActorTransicion.SISTEMA, null,
                    "El operador no la atendió en 24 horas", ahora);
        } else {
            String motivo = intentoPagoEnCurso
                    ? "Terminó la ventana de confirmación y pago y la gracia del intento en curso"
                    : "Terminó la ventana de confirmación y pago";
            cerrarComoVencida(motivo, ahora);
        }
        return true;
    }

    // ---- Pagos (RF-50, RF-55, RF-58)

    /**
     * Un intento de pago empieza en la pasarela: mientras no se conozca su resultado, el vencimiento espera hasta
     * 30 min (RF-55). Solo dentro de la ventana y con intentos disponibles.
     */
    public ResultadoEventoPago marcarIntentoEnCurso(Instant ahora) {
        if (estado != CONFIRMADA && estado != PAGO_FALLIDO) {
            throw new TransicionInvalidaException(estado, "un intento de pago");
        }
        exigirDentroDelPlazo(ahora);
        if (intentoPagoEnCurso) {
            return ResultadoEventoPago.SIN_CAMBIOS;
        }
        intentoPagoEnCurso = true;
        actualizadaEn = micros(ahora);
        cambios++;
        return ResultadoEventoPago.APLICADO;
    }

    /**
     * El intento de pago quedó abandonado, sin resultado de la pasarela (DN-20): deja de aplazar el vencimiento, que se
     * aplica en la siguiente vuelta si la ventana ya terminó. No cuenta como rechazo (RF-58).
     */
    public ResultadoEventoPago liberarIntentoEnCurso(Instant ahora) {
        if (!intentoPagoEnCurso || estado.esTerminal()) {
            return ResultadoEventoPago.SIN_CAMBIOS;
        }
        intentoPagoEnCurso = false;
        actualizadaEn = micros(ahora);
        cambios++;
        return ResultadoEventoPago.APLICADO;
    }

    /**
     * La pasarela aprobó el pago (RF-50). Sobre una solicitud ya Pagada no hace nada. Sobre una cerrada, o cuya ventana
     * ya terminó, no la revive: la cierra como Vencida si hacía falta y responde pago tardío.
     */
    public ResultadoEventoPago registrarPagoAprobado(Instant ahora) {
        if (estado == PAGADA) {
            return ResultadoEventoPago.SIN_CAMBIOS;
        }
        if (estado.esTerminal()) {
            return ResultadoEventoPago.PAGO_TARDIO;
        }
        exigirTransicion(PAGADA);
        if (vencer(ahora)) {
            return ResultadoEventoPago.PAGO_TARDIO;
        }
        intentoPagoEnCurso = false;
        venceEn = null;
        transicionar(PAGADA, ActorTransicion.PAGOS, null, null, ahora);
        return ResultadoEventoPago.APLICADO;
    }

    /**
     * La pasarela rechazó el pago (RF-58). Queda con el pago fallido para reintentar dentro de la ventana; si era el
     * tercer intento, o la ventana ya había terminado mientras se esperaba el resultado, vence (RF-55, RF-58).
     */
    public ResultadoEventoPago registrarPagoRechazado(Instant ahora) {
        if (estado.esTerminal()) {
            return ResultadoEventoPago.SIN_CAMBIOS;
        }
        exigirTransicion(PAGO_FALLIDO);
        intentoPagoEnCurso = false;
        intentosPagoFallidos++;
        if (intentosPagoFallidos >= INTENTOS_PAGO) {
            cerrarComoVencida("La pasarela rechazó el tercer intento de pago", ahora);
        } else if (!ahora.isBefore(venceEn)) {
            cerrarComoVencida("La pasarela rechazó el pago después de terminar la ventana", ahora);
        } else {
            transicionar(PAGO_FALLIDO, ActorTransicion.PAGOS, null,
                    "Intento %d de %d rechazado".formatted(intentosPagoFallidos, INTENTOS_PAGO), ahora);
        }
        return ResultadoEventoPago.APLICADO;
    }

    // ---- Reglas internas

    private void cerrarComoVencida(String motivo, Instant ahora) {
        intentoPagoEnCurso = false;
        venceEn = null;
        transicionar(VENCIDA, ActorTransicion.SISTEMA, null, motivo, ahora);
    }

    public static boolean esTransicionValida(EstadoSolicitud de, EstadoSolicitud a) {
        return TRANSICIONES.getOrDefault(de, Set.of()).contains(a);
    }

    private void exigirTransicion(EstadoSolicitud destino) {
        if (!esTransicionValida(estado, destino)) {
            throw new TransicionInvalidaException(estado, destino);
        }
    }

    /** Pasado el plazo, nadie la cambia aunque el vencimiento programado todavía no haya corrido. */
    private void exigirDentroDelPlazo(Instant ahora) {
        if (venceEn != null && !ahora.isBefore(venceEn)) {
            throw new PlazoVencidoException(venceEn);
        }
    }

    private void transicionar(EstadoSolicitud destino, ActorTransicion actor, UUID actorId, String motivo,
                              Instant ahora) {
        EstadoSolicitud origen = estado;
        estado = destino;
        actualizadaEn = micros(ahora);
        cambios++;
        registrar(new TransicionSolicitud(origen, destino, ahora, actor, actorId, motivo));
    }

    private void registrar(TransicionSolicitud transicion) {
        historial.add(transicion);
        notificaciones.addAll(ReglaNotificaciones.para(this, transicion));
    }

    private static Instant micros(Instant instante) {
        return instante == null ? null : instante.truncatedTo(ChronoUnit.MICROS);
    }

    // ---- Consultas

    /**
     * Cuándo debe aplicarse el vencimiento: el plazo vigente, o 30 min después si hay un intento de pago en curso.
     * Vacío si está cerrada.
     */
    public Instant limiteVencimiento() {
        if (venceEn == null || estado.esTerminal()) {
            return null;
        }
        return intentoPagoEnCurso ? PoliticaVentanas.conGracia(venceEn) : venceEn;
    }

    /** Para la cola del operador: pendiente y con 6 h o menos para atenderla (RF-36). */
    public boolean proximaAVencer(Instant ahora) {
        return estado == PENDIENTE && PoliticaVentanas.proximaAVencer(venceEn, ahora);
    }

    /**
     * El inicio del servicio en la hora de Colombia: la fecha de inicio a la hora de salida o de la reserva; en
     * alojamiento, que no tiene hora, el comienzo del día [SUPUESTO] (DN-10).
     */
    public Instant inicioServicio() {
        LocalTime hora = campos.hora() == null ? LocalTime.MIDNIGHT : campos.hora();
        return fechaInicio.atTime(hora).atZone(PoliticaVentanas.ZONA).toInstant();
    }

    public boolean esDelTurista(UUID turista) {
        return turistaId.equals(turista);
    }

    public boolean esDelOperador(UUID operador) {
        return operadorId.equals(operador);
    }

    /** Si cambió desde que se creó o se cargó: si hay algo que guardar. */
    public boolean tieneCambios() {
        return cambios > 0;
    }

    /** Las notificaciones de los cambios hechos desde que se creó o se cargó; las entrega una sola vez. */
    public List<NotificacionSolicitud> extraerNotificaciones() {
        List<NotificacionSolicitud> extraidas = List.copyOf(notificaciones);
        notificaciones.clear();
        return extraidas;
    }

    public UUID id() {
        return id;
    }

    public UUID turistaId() {
        return turistaId;
    }

    public String ofertaId() {
        return ofertaId;
    }

    public DatosOfertaCopiados oferta() {
        return oferta;
    }

    public LocalDate fechaInicio() {
        return fechaInicio;
    }

    public CamposDelTipo campos() {
        return campos;
    }

    /** Solo actividad y paquete. */
    public LocalTime horaSalida() {
        return campos.horaSalida();
    }

    /** Solo restaurante. */
    public LocalTime horaReserva() {
        return campos.horaReserva();
    }

    /** Solo alojamiento. */
    public LocalDate fechaSalida() {
        return campos.fechaSalida();
    }

    /** Solo alojamiento. */
    public Integer numeroUnidades() {
        return campos.numeroUnidades();
    }

    public int personas() {
        return personas;
    }

    public String notaTurista() {
        return notaTurista;
    }

    public UUID operadorId() {
        return operadorId;
    }

    public Instant creadaEn() {
        return creadaEn;
    }

    public EstadoSolicitud estado() {
        return estado;
    }

    /** Vacías hasta que el operador la atiende. */
    public CondicionesOfrecidas condiciones() {
        return condiciones;
    }

    /**
     * Hasta cuándo el prestador aparta el cupo: el fin de la ventana de confirmación y pago, registrado al atenderla
     * (RF-38). Queda en la solicitud aunque se cierre; vacío si nunca se atendió.
     */
    public Instant aparteHasta() {
        return aparteHasta;
    }

    /** El plazo vigente: el del operador mientras está Pendiente, luego la ventana; vacío si está cerrada. */
    public Instant venceEn() {
        return venceEn;
    }

    public boolean intentoPagoEnCurso() {
        return intentoPagoEnCurso;
    }

    public int intentosPagoFallidos() {
        return intentosPagoFallidos;
    }

    public Instant actualizadaEn() {
        return actualizadaEn;
    }

    /** La revisión leída de la base para el bloqueo optimista (ADR-05); vacía si todavía no se guardó. */
    public Long revision() {
        return revision;
    }

    public List<TransicionSolicitud> historial() {
        return List.copyOf(historial);
    }
}
