package co.edu.unimagdalena.samariantour.catalogo.opiniones.dominio.modelo;

import co.edu.unimagdalena.samariantour.catalogo.opiniones.dominio.excepcion.DatoInvalidoException;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Opinión de un turista sobre una oferta que pagó (RF-60): una calificación de 1 a 5 y un comentario. Nace en
 * moderación y solo se publica cuando el administrador la aprueba (RF-61, RF-80). Una rechazada queda con su motivo y
 * no se edita (DC-41).
 *
 * <p>La moderación es idempotente, como la de las ofertas (DC-08): aprobar o rechazar una opinión que ya no está en
 * moderación no cambia nada, para que el aviso repetido o atrasado del núcleo se ignore sin error.
 */
public final class Opinion {

    /** Revisión de una opinión que todavía no se guardó (bloqueo optimista del ADR-05). */
    public static final long REVISION_NUEVA = 0;

    public static final int CALIFICACION_MINIMA = 1;
    public static final int CALIFICACION_MAXIMA = 5;
    public static final int COMENTARIO_MINIMO = 10;
    public static final int COMENTARIO_MAXIMO = 1000;

    private final String id;
    private final String ofertaId;
    private final UUID turistaId;
    private final int calificacion;
    private final String comentario;
    private final Instant fecha;
    private final long revision;
    private EstadoOpinion estado;
    private String motivoRechazo;
    private Instant revisadaEn;

    private Opinion(String id, String ofertaId, UUID turistaId, int calificacion, String comentario, Instant fecha,
                    EstadoOpinion estado, String motivoRechazo, Instant revisadaEn, long revision) {
        this.id = Validaciones.texto(id, "id");
        this.ofertaId = Validaciones.texto(ofertaId, "ofertaId");
        this.turistaId = Objects.requireNonNull(turistaId, "turistaId");
        this.calificacion = calificacion(calificacion);
        this.comentario = comentario(comentario);
        this.fecha = Objects.requireNonNull(fecha, "fecha");
        this.estado = Objects.requireNonNull(estado, "estado");
        this.motivoRechazo = motivoRechazo;
        this.revisadaEn = revisadaEn;
        this.revision = revision;
    }

    /**
     * La opinión que el turista envía: queda en moderación.
     *
     * @throws DatoInvalidoException si la calificación no está entre 1 y 5 o el comentario no tiene entre 10 y 1000
     *                               caracteres sin los espacios de los extremos
     */
    public static Opinion enviar(String id, String ofertaId, UUID turistaId, Integer calificacion, String comentario,
                                 Instant fecha) {
        return new Opinion(id, ofertaId, turistaId, Validaciones.presente(calificacion, "calificacion"), comentario,
                fecha, EstadoOpinion.EN_MODERACION, null, null, REVISION_NUEVA);
    }

    /** Reconstruye una opinión guardada; solo para los adaptadores de persistencia. */
    public static Opinion reconstituir(String id, String ofertaId, UUID turistaId, int calificacion, String comentario,
                                       Instant fecha, EstadoOpinion estado, String motivoRechazo, Instant revisadaEn,
                                       long revision) {
        if ((estado == EstadoOpinion.EN_MODERACION) != (revisadaEn == null)) {
            throw new IllegalArgumentException("Una opinión tiene fecha de revisión si y solo si ya se moderó");
        }
        if ((estado == EstadoOpinion.RECHAZADA) != (motivoRechazo != null)) {
            throw new IllegalArgumentException("Una opinión tiene motivo de rechazo si y solo si fue rechazada");
        }
        return new Opinion(id, ofertaId, turistaId, calificacion, comentario, fecha, estado, motivoRechazo,
                revisadaEn, revision);
    }

    /**
     * Publica la opinión (RF-61).
     *
     * @return true si cambió; false si ya no estaba en moderación
     */
    public boolean aprobar(Instant ahora) {
        if (!estaEnModeracion()) {
            return false;
        }
        estado = EstadoOpinion.APROBADA;
        revisadaEn = Objects.requireNonNull(ahora, "ahora");
        return true;
    }

    /**
     * Rechaza la opinión con un motivo que el turista ve (RF-80).
     *
     * @return true si cambió; false si ya no estaba en moderación
     * @throws DatoInvalidoException si falta el motivo
     */
    public boolean rechazar(String motivo, Instant ahora) {
        String motivoValido = Validaciones.textoOpcional(motivo);
        if (motivoValido == null) {
            throw new DatoInvalidoException("motivo", "es obligatorio al rechazar");
        }
        if (!estaEnModeracion()) {
            return false;
        }
        estado = EstadoOpinion.RECHAZADA;
        motivoRechazo = motivoValido;
        revisadaEn = Objects.requireNonNull(ahora, "ahora");
        return true;
    }

    public boolean estaEnModeracion() {
        return estado == EstadoOpinion.EN_MODERACION;
    }

    public boolean estaAprobada() {
        return estado == EstadoOpinion.APROBADA;
    }

    private static int calificacion(int valor) {
        if (valor < CALIFICACION_MINIMA || valor > CALIFICACION_MAXIMA) {
            throw new DatoInvalidoException("calificacion",
                    "debe estar entre %d y %d".formatted(CALIFICACION_MINIMA, CALIFICACION_MAXIMA));
        }
        return valor;
    }

    private static String comentario(String valor) {
        String texto = Validaciones.texto(valor, "comentario");
        if (texto.length() < COMENTARIO_MINIMO || texto.length() > COMENTARIO_MAXIMO) {
            throw new DatoInvalidoException("comentario",
                    "debe tener entre %d y %d caracteres".formatted(COMENTARIO_MINIMO, COMENTARIO_MAXIMO));
        }
        return texto;
    }

    public String id() {
        return id;
    }

    public String ofertaId() {
        return ofertaId;
    }

    public UUID turistaId() {
        return turistaId;
    }

    public int calificacion() {
        return calificacion;
    }

    public String comentario() {
        return comentario;
    }

    /** Cuándo el turista la envió. */
    public Instant fecha() {
        return fecha;
    }

    public EstadoOpinion estado() {
        return estado;
    }

    /** Solo existe si fue rechazada. */
    public String motivoRechazo() {
        return motivoRechazo;
    }

    /** Solo existe si ya se moderó. */
    public Instant revisadaEn() {
        return revisadaEn;
    }

    /** Revisión del bloqueo optimista (ADR-05); la sube el adaptador al guardar. */
    public long revision() {
        return revision;
    }
}
